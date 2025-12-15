(ns renderer.tool.impl.base.edit
  (:require
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.element :as utils.element]
   [renderer.utils.svg :as utils.svg]
   [renderer.views :as views]))

(defmethod tool.hierarchy/properties :edit
  []
  {:icon "edit"
   :label [::label "Edit"]})

(defmethod tool.hierarchy/help [:edit :idle]
  []
  [:<>
   (i18n.views/t [::help-idle-drag "Drag a handle to modify your shape."])
   (i18n.views/t [::help-idle-click
                  "Click on an element to change selection"])])

(defmethod tool.hierarchy/help [:edit :edit]
  []
  (i18n.views/t [::help-edit "Hold %1 to restrict direction."]
                [[views/kbd "Ctrl"]]))

(defmethod tool.hierarchy/help [:edit :type]
  []
  (i18n.views/t [::help-type "Enter your text."]))

(defmethod tool.hierarchy/on-pointer-down :edit
  [db e]
  (let [{:keys [element]} e]
    (cond-> db
      element
      (assoc :clicked-element element))))

(defmethod tool.hierarchy/on-pointer-up :edit
  [db e]
  (let [{:keys [shift-key button element]} e]
    (if-not (and (= button :right)
                 (:selected element))
      (-> db
          (element.handlers/clear-ignored)
          (dissoc :clicked-element)
          (element.handlers/toggle-selection (:id element) shift-key)
          (history.handlers/finalize [::select-element "Select element"]))
      (dissoc db :clicked-element))))

(defmethod tool.hierarchy/on-pointer-move :edit
  [db e]
  (let [el-id (-> e :element :id)]
    (cond-> db
      :always
      (element.handlers/clear-hovered)

      el-id
      (element.handlers/hover el-id))))

(defmethod tool.hierarchy/on-drag-start :edit
  [db e]
  (tool.handlers/set-state db (if (= (-> e :element :type) :handle)
                                :edit
                                :select)))

(defmethod tool.hierarchy/on-drag :edit
  [db e]
  (let [{:keys [element-id id]} (:clicked-element db)
        offset (matrix/add (tool.handlers/pointer-delta db)
                           (snap.handlers/nearest-delta db))]
    (cond-> db
      :always
      (history.handlers/reset-state)

      element-id
      (element.handlers/update-el element-id
                                  element.hierarchy/edit offset id e))))

(defmethod tool.hierarchy/on-drag-end :edit
  [db _e]
  (-> db
      (tool.handlers/set-state :idle)
      (dissoc :clicked-element)
      (history.handlers/finalize [::edit "Edit"])))

(defmethod tool.hierarchy/snapping-points :edit
  [db]
  (when-let [el (:clicked-element db)]
    [(with-meta
       (matrix/add [(:x el) (:y el)]
                   (tool.handlers/pointer-delta db))
       {:label (when (= (:type el) :handle)
                 (or (:label el)
                     [::handle "handle"]))})]))

(defmethod tool.hierarchy/snapping-elements :edit
  [db]
  (element.handlers/non-selected-visible db))

(defmethod tool.hierarchy/render :edit
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
