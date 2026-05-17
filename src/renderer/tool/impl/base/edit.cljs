(ns renderer.tool.impl.base.edit
  (:require
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.input.handlers :as input.handlers]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.element :as utils.element]
   [renderer.utils.key :as utils.key]
   [renderer.utils.svg :as utils.svg]
   [renderer.views :as views]))

(hierarchy/derive! ::edit ::tool.hierarchy/tool)

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

(defmethod tool.hierarchy/on-pointer-up [::edit :idle]
  [db e]
  (let [{:keys [shift-key ctrl-key element]} e]
    (cond
      ctrl-key
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
  (cond-> db
    (= (-> db :clicked-element :type) :handle)
    (tool.handlers/set-state :edit)))

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

(defmethod tool.hierarchy/on-drag-end [::edit :edit]
  [db e]
  (-> db
      (tool.handlers/set-state :idle)
      (dissoc :clicked-element)
      (history.handlers/finalize (:timestamp e) [::edit "Edit"])))

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
         (into [:g]))))

(rf/dispatch [::action.events/register-action
              {:id :tool/edit
               :label [::tool-edit "Edit"]
               :icon "edit"
               :event [::tool.events/activate ::edit]
               :active [::tool.subs/active? ::edit]
               :shortcuts [{:keyCode (utils.key/codes "E")}]}])
