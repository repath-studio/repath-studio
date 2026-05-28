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

(defmethod tool.hierarchy/on-pointer-down [::edit/edit :idle]
  [db e]
  (let [{:keys [element]} e]
    (cond-> db
      element
      (assoc :clicked-element element))))

(defmethod tool.hierarchy/on-pointer-up [::edit/edit :idle]
  [db e]
  (let [{:keys [shift-key element]} e
        {:keys [id parent]} element]
    (cond-> db
      :always
      (dissoc :clicked-element)

      (= (:type element) :handle)
      (-> (element.handlers/toggle-handle-selection parent id shift-key)
          (history.handlers/finalize (:timestamp e)
                                     [::select-handle "Select handle"]))

      (= (:type element) :element)
      (-> (element.handlers/toggle-selection id shift-key)
          (history.handlers/finalize (:timestamp e)
                                     [::select-element "Select element"])))))

(defmethod tool.hierarchy/on-double-click [::edit/edit :idle]
  [db e]
  (let [{:keys [element]} e
        {:keys [parent id]} element]
    (cond-> db
      (= (:type element) :handle)
      (-> (dissoc :clicked-element)
          (element.handlers/update-el parent element.hierarchy/handle-click id)
          (history.handlers/finalize (:timestamp e) [::edit/label "Edit"])))))

(defmethod tool.hierarchy/on-pointer-move [::edit/edit :idle]
  [db e]
  (-> db
      (element.handlers/clear-hovered)
      (element.handlers/hover (-> e :element :id))))

(defmethod tool.hierarchy/on-drag-start [::edit/edit :idle]
  [db e]
  (let [{:keys [clicked-element]} db
        {:keys [shift-key]} e
        {:keys [id parent]} clicked-element]
    (if (= (:type clicked-element) :handle)
      (-> db
          (element.handlers/toggle-handle-selection parent id shift-key)
          (tool.handlers/set-state :edit))
      (tool.handlers/set-state db :select))))

(defmethod tool.hierarchy/snapping-elements [::edit/edit :idle]
  [db]
  (element.handlers/non-selected-visible db))
