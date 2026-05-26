(ns renderer.tool.impl.base.edit.idle
  (:require
   [renderer.document.handlers :as document.handlers]
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

(defmethod tool.hierarchy/on-pointer-down [::edit/edit :idle]
  [db e]
  (let [{:keys [element]} e]
    (cond-> db
      element
      (assoc :clicked-element element))))

(defmethod tool.hierarchy/on-pointer-up [::edit/edit :idle]
  [db e]
  (let [{:keys [shift-key element]} e]
    (case (:type element)
      :handle
      (document.handlers/toggle-handle-selection db (:id element) shift-key)

      :element
      (-> db
          (dissoc db :clicked-element)
          (element.handlers/clear-ignored)
          (element.handlers/toggle-selection (:id element) shift-key)
          (history.handlers/finalize (:timestamp e)
                                     [::select-element "Select element"]))

      db)))

(defmethod tool.hierarchy/on-double-click [::edit/edit :idle]
  [db e]
  (let [{:keys [element]} e]
    (cond-> db
      (= (:type element) :handle)
      (-> (dissoc :clicked-element)
          (element.handlers/update-el (:element-id element)
                                      element.hierarchy/edit-click
                                      (:id element))
          (history.handlers/finalize (:timestamp e) [::edit/label "Edit"])))))

(defmethod tool.hierarchy/on-pointer-move [::edit/edit :idle]
  [db e]
  (let [el-id (-> e :element :id)]
    (cond-> db
      :always
      (element.handlers/clear-hovered)

      el-id
      (element.handlers/hover el-id))))

(defmethod tool.hierarchy/on-drag-start [::edit/edit :idle]
  [db e]
  (let [{:keys [clicked-element]} db
        {:keys [active-document]} db
        {:keys [id]} clicked-element
        {:keys [selected-handles]} (get-in db [:documents active-document])
        selected? (contains? selected-handles id)]
    (cond
      (= (:type clicked-element) :handle)
      (cond-> db
        (not selected?)
        (document.handlers/toggle-handle-selection id (:shift-key e))

        :always
        (tool.handlers/set-state :edit))

      :else
      (tool.handlers/set-state db :select))))

(defmethod tool.hierarchy/snapping-elements [::edit/edit :idle]
  [db]
  (element.handlers/non-selected-visible db))
