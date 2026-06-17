(ns renderer.tool.impl.base.transform.idle
  (:require
   [malli.core :as m]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.db :refer [Handle State]]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.transform.core :as-alias transform]
   [renderer.tool.impl.base.transform.select :as transform.select]
   [renderer.utils.element :as utils.element]
   [renderer.utils.key :as utils.key]
   [renderer.views :as views]))

(defmethod tool.hierarchy/on-pointer-move [::transform/transform :idle]
  [db e]
  (let [{:keys [element]} e
        movable? (or (= (:type element) :handle)
                     (and element (not (utils.element/root? element))))
        cursor (if movable? "move" "default")]
    (cond-> db
      :always
      (-> (element.handlers/clear-hovered)
          (tool.handlers/set-cursor cursor))

      (:id element)
      (element.handlers/hover (:id element)))))

(defmethod tool.hierarchy/help [::transform/transform :idle]
  []
  [:<>
   (i18n.views/t [::idle-click
                  [:div "Click to select an element or drag to select by
                         area."]])
   (i18n.views/t [::idle-hold
                  [:div "Hold %1 to add or remove elements to selection."]]
                 [[views/kbd "⇧"]])])

(defmethod tool.hierarchy/on-pointer-down [::transform/transform :idle]
  [db e]
  (let [{:keys [button element]} e
        {:keys [selected id]} element]
    (cond-> db
      element
      (assoc :clicked-element element)

      (and (= button :right) (not selected))
      (element.handlers/toggle-selection id (:shift-key e)))))

(defmethod tool.hierarchy/on-pointer-up [::transform/transform :idle]
  [db e]
  (let [{:keys [element timestamp shift-key]} e
        {:keys [selected id]} element]
    (-> db
        (dissoc :clicked-element)
        (element.handlers/toggle-selection id shift-key)
        (history.handlers/finalize timestamp
                                   (if selected
                                     [::deselect-element "Deselect element"]
                                     [::select-element "Select element"])))))

(defmethod tool.hierarchy/on-double-click [::transform/transform :idle]
  [db e]
  (let [{{:keys [timestamp tag id]} :element} e]
    (if (= tag :g)
      (-> db
          (element.handlers/ignore id)
          (element.handlers/deselect id)
          (history.handlers/finalize timestamp
                                     [::deselect-element "Deselect element"]))
      (cond-> db
        (not= :canvas tag)
        (tool.handlers/edit)))))

(m/=> drag-start->state [:-> [:or Element Handle] State])
(defn drag-start->state
  [el]
  (case (:type el)
    :element
    (if (utils.element/root? el)
      :select
      :translate)

    :handle
    (:action el)

    :idle))

(defmethod tool.hierarchy/on-drag-start [::transform/transform :idle]
  [db e]
  (let [{:keys [clicked-element state]} db
        {:keys [shift-key]} e
        {:keys [id]} clicked-element
        new-state (drag-start->state clicked-element)]
    (cond-> db
      :always
      (-> (tool.handlers/set-state new-state)
          (element.handlers/clear-hovered))

      (not= state new-state)
      (snap.handlers/rebuild-tree)

      (transform.select/selectable? clicked-element)
      (-> (element.handlers/toggle-selection id shift-key)
          (snap.handlers/delete-from-tree #{id})))))

(defn event->arrow-key-step
  [e]
  (let [{:keys [shift-key ctrl-key]} e
        step 10]
    (cond-> 1
      shift-key (* step)
      ctrl-key (/ step))))

(defn event->offset
  [e]
  (let [arrow-key-step (event->arrow-key-step e)]
    (case (:key e)
      "ArrowUp" [0 (- arrow-key-step)]
      "ArrowDown" [0 arrow-key-step]
      "ArrowLeft" [(- arrow-key-step) 0]
      "ArrowRight" [arrow-key-step 0]
      [0 0])))

(defmethod tool.hierarchy/on-key-down [::transform/transform :idle]
  [db e]
  (let [k (:key e)]
    (cond-> db
      (utils.key/arrow? k)
      (element.handlers/translate (event->offset e))

      (= k "Escape")
      (history.handlers/reset-state))))

(defmethod tool.hierarchy/on-key-up [::transform/transform :idle]
  [db e]
  (let [k (:key e)]
    (cond-> db
      (utils.key/arrow? k)
      (history.handlers/finalize (:timestamp e)
                                 [::move-selection "Move selection"]))))

(defmethod tool.hierarchy/on-delete [::transform/transform :idle]
  [db]
  (element.handlers/delete db))
