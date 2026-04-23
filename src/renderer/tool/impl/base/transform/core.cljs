(ns renderer.tool.impl.base.transform.core
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.app.subs :as-alias app.subs]
   [renderer.db :refer [Orientation Vec2]]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.subs :as-alias element.subs]
   [renderer.history.handlers :as history.handlers]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.transform.clone :as transform.clone]
   [renderer.tool.impl.base.transform.idle]
   [renderer.tool.impl.base.transform.scale :as transform.scale]
   [renderer.tool.impl.base.transform.select :as transform.select]
   [renderer.tool.impl.base.transform.translate :as transform.translate]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.tool.views :as tool.views]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.key :as utils.key]
   [renderer.utils.svg :as utils.svg]))

(tool.hierarchy/derive-tool :transform ::tool.hierarchy/tool)

(defmethod tool.hierarchy/on-pointer-move :transform
  [db e]
  (let [{:keys [element]} e
        movable? (or (= (:type element) :handle)
                     (and element (not (utils.element/root? element))))
        cursor (if movable? "move" "default")]
    (cond-> db
      (not (:shift-key e))
      (element.handlers/clear-ignored)

      :always
      (-> (element.handlers/clear-hovered)
          (tool.handlers/set-cursor cursor))

      (:id element)
      (element.handlers/hover (:id element)))))

(defn event->arrow-key-step
  [e]
  (let [{:keys [shift-key ctrl-key]} e]
    (cond-> 1
      shift-key (* 10)
      ctrl-key (/ 10))))

(defn event->offset
  [e]
  (let [arrow-key-step (event->arrow-key-step e)]
    (case (:key e)
      "ArrowUp" [0 (- arrow-key-step)]
      "ArrowDown" [0 arrow-key-step]
      "ArrowLeft" [(- arrow-key-step) 0]
      "ArrowRight" [arrow-key-step 0]
      [0 0])))

(defmethod tool.hierarchy/on-key-down :transform
  [db e]
  (let [k (:key e)]
    (cond-> db
      (= k "Shift")
      (element.handlers/ignore :bbox)

      (utils.key/arrow? k)
      (element.handlers/translate (event->offset e))

      (= k "Escape")
      (history.handlers/reset-state))))

(defmethod tool.hierarchy/on-key-up :transform
  [db e]
  (let [k (:key e)]
    (cond-> db
      (= k "Shift")
      (element.handlers/clear-ignored)

      (utils.key/arrow? k)
      (history.handlers/finalize (:timestamp e)
                                 [::move-selection "Move selection"]))))

(defmethod tool.hierarchy/on-pointer-down :transform
  [db e]
  (let [{:keys [button element]} e]
    (cond-> db
      element
      (assoc :clicked-element element)

      (and (= button :right) (not= (:id element) :bbox))
      (element.handlers/toggle-selection (:id element) (:shift-key e))

      :always
      (element.handlers/ignore :bbox))))

(defmethod tool.hierarchy/on-pointer-up :transform
  [db e]
  (let [{:keys [element timestamp]} e]
    (-> db
        (dissoc :clicked-element)
        (element.handlers/unignore :bbox)
        (element.handlers/toggle-selection (:id element) (:shift-key e))
        (history.handlers/finalize timestamp
                                   (if (:selected element)
                                     [::deselect-element "Deselect element"]
                                     [::select-element "Select element"])))))

(defmethod tool.hierarchy/on-double-click :transform
  [db e]
  (let [{{:keys [tag id]} :element} e]
    (if (= tag :g)
      (-> db
          (element.handlers/ignore id)
          (element.handlers/deselect id))
      (cond-> db
        (not= :canvas tag)
        (tool.handlers/activate :edit)))))

(defmethod tool.hierarchy/on-deactivate :transform
  [db]
  (-> db
      (element.handlers/clear-ignored)
      (element.handlers/clear-hovered)
      (dissoc :pivot-point)
      (transform.select/clear-select-box)))

(defn drag-start->state
  [clicked-element]
  (let [{el-type :type
         :keys [tag action]} clicked-element]
    (case el-type
      :element
      (if (= tag :canvas)
        :select
        :translate)

      :handle
      (if (= action :scale)
        :scale
        :translate)

      :idle)))

(defmethod tool.hierarchy/on-drag-start :transform
  [db e]
  (let [{:keys [clicked-element]} db
        {:keys [shift-key]} e
        {:keys [id]} clicked-element
        state (drag-start->state clicked-element)]
    (cond-> db
      :always
      (-> (tool.handlers/set-state state)
          (element.handlers/clear-hovered))

      (transform.select/selectable? clicked-element)
      (-> (element.handlers/toggle-selection id shift-key)
          (snap.handlers/delete-from-tree #{id})))))

(m/=> direction [:-> Vec2 Orientation])
(defn direction
  [delta]
  (let [[delta-x delta-y] delta]
    (if (> (abs delta-x) (abs delta-y))
      :vertical
      :horizontal)))

(defmethod tool.hierarchy/on-drag :transform
  [db e]
  (let [{:keys [ctrl-key alt-key]} e
        db (element.handlers/clear-ignored db)
        delta (tool.handlers/pointer-delta db)
        axis (when ctrl-key (direction delta))]
    (case (:state db)
      :select
      (transform.select/on-drag db e)

      :translate
      (if alt-key
        (tool.handlers/set-state db :clone)
        (transform.translate/on-drag db delta axis e))

      :clone
      (if alt-key
        (transform.clone/on-drag db axis e)
        (tool.handlers/set-state db :translate))

      :scale
      (transform.scale/on-drag db delta e)

      db)))

(defmethod tool.hierarchy/on-drag-end :transform
  [db e]
  (let [{:keys [state]} db]
    (cond-> db
      (= state :select)
      (transform.select/on-drag-end e)

      (not= state :idle)
      (history.handlers/finalize
       (:timestamp e)
       (case state
         :select [::modify-selection "Modify selection"]
         :translate [::move-selection "Move selection"]
         :scale [::scale-selection "Scale selection"]
         :clone [::clone-selection "Clone selection"]))

      :always
      (-> (tool.handlers/set-state :idle)
          (element.handlers/clear-hovered)
          (dissoc :clicked-element :pivot-point)))))

(defmethod tool.hierarchy/snapping-points :transform
  [db]
  (let [selected (element.handlers/selected db)
        options (-> db :snap :options)]
    (cond
      (= (:state db) :scale)
      (when-let [el (:clicked-element db)]
        [(with-meta
           (matrix/add [(:x el) (:y el)]
                       (tool.handlers/pointer-delta db))
           {:label [::scale-handle "scale handle"]})])

      (not= (:state db) :idle)
      (cond-> (element.handlers/snapping-points db (filter :visible selected))
        (seq (rest selected))
        (into (utils.bounds/->snapping-points (element.handlers/bbox db)
                                              options))))))

(defmethod tool.hierarchy/snapping-elements :transform
  [db]
  (element.handlers/non-selected-visible db))

(m/=> render-bounding-box [:-> Element boolean? any?])
(defn render-bounding-box
  [el dashed?]
  (some-> (:bbox el)
          (utils.svg/bounding-box dashed?)))

(defmethod tool.hierarchy/render :transform
  []
  (let [state @(rf/subscribe [::tool.subs/state])
        selected-elements @(rf/subscribe [::element.subs/selected])
        bbox @(rf/subscribe [::element.subs/bbox])
        pivot-point @(rf/subscribe [::tool.subs/pivot-point])
        hovered-elements @(rf/subscribe [::element.subs/hovered])
        touch? @(rf/subscribe [::app.subs/supported-feature? :touch])]
    [:<>
     (into [:<>]
           (map #(render-bounding-box % false) selected-elements))

     (when (or (not touch?) (= state :select))
       (into [:<>]
             (map #(render-bounding-box % true) hovered-elements)))

     (when (seq bbox)
       [:<>
        [tool.views/wrapping-bbox bbox]
        [tool.views/bounding-corners bbox]])

     (when (and (= state :scale) (seq bbox))
       [:<>
        [transform.scale/area-label bbox]
        [transform.scale/size-label bbox]])

     (when pivot-point
       [utils.svg/times pivot-point])

     [transform.select/render-select-box]]))

(rf/dispatch [::action.events/register-action
              {:id :tool/transform
               :label [::label "Transform"]
               :icon "pointer"
               :event [::tool.events/activate :transform]
               :active [::tool.subs/active? :transform]
               :shortcuts [{:keyCode (utils.key/codes "S")}]}])
