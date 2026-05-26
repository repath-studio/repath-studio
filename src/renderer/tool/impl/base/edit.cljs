(ns renderer.tool.impl.base.edit
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.action.events :as-alias action.events]
   [renderer.app.db :refer [App]]
   [renderer.app.handlers :as app.handlers]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.input.db :refer [PointerEvent]]
   [renderer.input.handlers :as input.handlers]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.db :refer [Handle]]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.key :as utils.key]
   [renderer.utils.svg :as utils.svg]
   [renderer.views :as views]))

(hierarchy/derive! ::edit ::tool.hierarchy/tool)

(defonce select-box (reagent/atom nil))

(rf/reg-fx
 ::set-select-box
 (fn [value]
   (reset! select-box value)))

(m/=> selectable? [:-> [:or Element Handle nil?] boolean?])
(defn selectable?
  [el]
  (and (= :handle (:type el))
       (not (:selected el))
       (not= :canvas (:tag el))))

(defmethod tool.hierarchy/help [::edit :idle]
  []
  [:<>
   (i18n.views/t [::help-idle-drag "Drag a handle to modify your shape."])
   (i18n.views/t [::help-idle-click
                  "Click on an element to change selection"])])

(defmethod tool.hierarchy/help [::edit :edit]
  []
  (i18n.views/t [::help-edit "Hold %1 to restrict direction."]
                [[views/kbd "Ctrl"]]))

(defmethod tool.hierarchy/help [::edit :type]
  []
  (i18n.views/t [::help-type "Enter your text."]))

(defmethod tool.hierarchy/on-pointer-down [::edit :idle]
  [db e]
  (let [{:keys [element]} e]
    (cond-> db
      element
      (assoc :clicked-element element))))

(defn edit-click
  [db e]
  (let [{:keys [element]} e]
    (cond-> db
      (= (:type element) :handle)
      (-> (dissoc :clicked-element)
          (element.handlers/update-el (:element-id element)
                                      element.hierarchy/edit-click
                                      (:id element))
          (history.handlers/finalize (:timestamp e) [::edit "Edit"])))))

(defn clear-select-box
  [db]
  (app.handlers/add-fx db [::set-select-box nil]))

(defmethod tool.hierarchy/on-pointer-up [::edit :idle]
  [db e]
  (let [{:keys [shift-key ctrl-key element]} e]
    (cond
      ctrl-key
      (edit-click db e)

      (= (:type element) :handle)
      (edit-click db e)

      :else
      (-> db
          (dissoc db :clicked-element)
          (element.handlers/clear-ignored)
          (element.handlers/toggle-selection (:id element) shift-key)
          (history.handlers/finalize (:timestamp e)
                                     [::select-element "Select element"])))))

(defmethod tool.hierarchy/on-double-click [::edit :idle]
  [db e]
  (edit-click db e))

(defmethod tool.hierarchy/on-pointer-move [::edit :idle]
  [db e]
  (let [el-id (-> e :element :id)]
    (cond-> db
      :always
      (element.handlers/clear-hovered)

      el-id
      (element.handlers/hover el-id))))

(defmethod tool.hierarchy/on-drag-start [::edit :idle]
  [db _e]
  (cond
    (= (-> db :clicked-element :type) :handle)
    (tool.handlers/set-state db :edit)

    :else
    (tool.handlers/set-state db :select)))

(defmethod tool.hierarchy/on-drag [::edit :edit]
  [db e]
  (let [{:keys [element-id id]} (:clicked-element db)
        lock? (or (:ctrl-key e) (input.handlers/multi-touch? db))
        offset (matrix/add (tool.handlers/pointer-delta db)
                           (snap.handlers/nearest-delta db))]
    (cond-> db
      :always
      (history.handlers/reset-state)

      element-id
      (element.handlers/update-el element-id
                                  element.hierarchy/edit-drag
                                  offset id lock?))))

(m/=> select-rect [:-> App boolean? Element])
(defn select-rect
  [db intersecting?]
  (cond-> (tool.handlers/select-box db)
    (not intersecting?)
    (assoc-in [:attrs :fill] "transparent")))

(m/=> hovered? [:-> Handle boolean? boolean?])
(defn hovered?
  [handle]
  (boolean (when-let [selection-bbox (element.hierarchy/bbox @select-box)]
             (utils.bounds/contained-point? selection-bbox
                                            [(:x handle) (:y handle)]))))

(m/=> reduce-by-area [:-> App PointerEvent ifn? App])
(defn reduce-by-area
  [db f]
  (transduce
   (comp
    (element.handlers/visible)
    (filter #(hovered? %))
    (map :id))
   (fn [db id]
     (cond-> db
       id (f id)))
   db (element.handlers/entities db)))

(defmethod tool.hierarchy/on-drag [::edit :select]
  [db e]
  (let [{:keys [alt-key]} e]
    (-> db
        (element.handlers/clear-hovered)
        (app.handlers/add-fx [::set-select-box (select-rect db alt-key)])
        (reduce-by-area element.handlers/hover))))

(defmethod tool.hierarchy/on-drag-end [::edit :edit]
  [db e]
  (-> db
      (tool.handlers/set-state :idle)
      (dissoc :clicked-element)
      (history.handlers/finalize (:timestamp e) [::edit "Edit"])))

(defmethod tool.hierarchy/on-drag-end [::edit :select]
  [db e]
  (cond-> db
    (not (:shift-key e))
    element.handlers/deselect

    :always
    (-> (reduce-by-area element.handlers/select)
        (clear-select-box)
        (tool.handlers/set-state :idle))))

(defmethod tool.hierarchy/snapping-points [::edit :edit]
  [db]
  (when-let [el (:clicked-element db)]
    [(with-meta
       (matrix/add [(:x el) (:y el)]
                   (tool.handlers/pointer-delta db))
       {:label (when (= (:type el) :handle)
                 (or (:label el)
                     [::handle "handle"]))})]))

(defmethod tool.hierarchy/snapping-elements [::edit :idle]
  [db]
  (element.handlers/non-selected-visible db))

(defmethod tool.hierarchy/snapping-elements [::edit :edit]
  [db]
  (element.handlers/non-selected-visible db))

(defmethod tool.hierarchy/render ::edit
  []
  (let [selected-elements @(rf/subscribe [::element.subs/selected])]
    (->> selected-elements
         (map (fn [el]
                [:g
                 [element.hierarchy/render-edit el]
                 (when-let [pos (element.hierarchy/centroid el)]
                   (let [offset (utils.element/offset el)
                         pos (matrix/add offset pos)]
                     [utils.svg/dot pos
                      [:title (i18n.views/t [::centroid "Centroid"])]]))]))
         (into [:g [element.hierarchy/render @select-box]]))))

(rf/dispatch [::action.events/register-action
              {:id :tool/edit
               :label [::tool-edit "Edit"]
               :icon "edit"
               :event [::tool.events/activate ::edit]
               :active [::tool.subs/active? ::edit]
               :shortcuts [{:keyCode (utils.key/codes "E")}]}])
