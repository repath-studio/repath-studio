(ns renderer.tool.impl.base.edit.idle
  (:require
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.edit.core :as-alias edit]))

(defmethod tool.hierarchy/help [::edit/edit :idle]
  []
  [:<>
   (i18n.views/t [::help-idle-drag "Drag a handle to modify your shape."])
   (i18n.views/t [::help-idle-click
                  "Click on an element to change selection"])])

(defn edit-click
  [db e]
  (let [{:keys [element]} e]
    (cond-> db
      (= (:type element) :handle)
      (-> (dissoc :clicked-element)
          (element.handlers/update-el (:element-id element)
                                      element.hierarchy/edit-click
                                      (:id element))
          (history.handlers/finalize (:timestamp e) [::edit/label "Edit"])))))

(defmethod tool.hierarchy/on-pointer-down [::edit/edit :idle]
  [db e]
  (let [{:keys [element]} e]
    (cond-> db
      element
      (assoc :clicked-element element))))

(defmethod tool.hierarchy/on-pointer-up [::edit/edit :idle]
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

(defmethod tool.hierarchy/on-double-click [::edit/edit :idle]
  [db e]
  (edit-click db e))

(defmethod tool.hierarchy/on-pointer-move [::edit/edit :idle]
  [db e]
  (let [el-id (-> e :element :id)]
    (cond-> db
      :always
      (element.handlers/clear-hovered)

      el-id
      (element.handlers/hover el-id))))

(defmethod tool.hierarchy/on-drag-start [::edit/edit :idle]
  [db _e]
  (cond
    (= (-> db :clicked-element :type) :handle)
    (tool.handlers/set-state db :edit)

    :else
    (tool.handlers/set-state db :select)))

(defmethod tool.hierarchy/snapping-elements [::edit/edit :idle]
  [db]
  (element.handlers/non-selected-visible db))
