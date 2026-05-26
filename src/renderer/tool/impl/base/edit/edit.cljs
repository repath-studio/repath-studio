(ns renderer.tool.impl.base.edit.edit
  (:require
   [clojure.core.matrix :as matrix]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.input.handlers :as input.handlers]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.edit.core :as-alias edit]
   [renderer.views :as views]))

(defmethod tool.hierarchy/help [::edit/edit :edit]
  []
  (i18n.views/t [::help-edit "Hold %1 to restrict direction."]
                [[views/kbd "Ctrl"]]))

(defn update-element
  [db element-id offset lock?]
  (let [{:keys [active-document]} db
        {:keys [selected-handles]} (get-in db [:documents active-document])]
    (reduce (fn [db id]
              (element.handlers/update-el db element-id
                                          element.hierarchy/edit-drag
                                          offset id lock?))
            db selected-handles)))

(defmethod tool.hierarchy/on-drag [::edit/edit :edit]
  [db e]
  (let [{:keys [element-id]} (:clicked-element db)
        lock? (or (:ctrl-key e) (input.handlers/multi-touch? db))
        offset (matrix/add (tool.handlers/pointer-delta db)
                           (snap.handlers/nearest-delta db))]
    (cond-> db
      :always
      (history.handlers/reset-state)

      element-id
      (update-element element-id offset lock?))))

(defmethod tool.hierarchy/on-drag-end [::edit/edit :edit]
  [db e]
  (-> db
      (tool.handlers/set-state :idle)
      (dissoc :clicked-element)
      (history.handlers/finalize (:timestamp e) [::edit/label "Edit"])))

(defmethod tool.hierarchy/snapping-points [::edit/edit :edit]
  [db]
  (when-let [el (:clicked-element db)]
    [(with-meta
       (matrix/add [(:x el) (:y el)]
                   (tool.handlers/pointer-delta db))
       {:label (when (= (:type el) :handle)
                 (or (:label el)
                     [::handle "handle"]))})]))

(defmethod tool.hierarchy/snapping-elements [::edit/edit :edit]
  [db]
  (element.handlers/non-selected-visible db))
