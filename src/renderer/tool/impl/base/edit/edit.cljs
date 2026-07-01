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
   [renderer.utils.extra :refer [rpartial]]
   [renderer.views :as views]))

(defmethod tool.hierarchy/help [::edit/edit :edit]
  []
  (i18n.views/t [::help-edit "Hold %1 to restrict direction."]
                [[views/kbd "Ctrl"]]))

(defn update-element
  [db el offset lock?]
  (->> (:selected-handles el)
       (reduce (fn [db handle-id]
                 (element.handlers/update-el db (:id el)
                                             element.hierarchy/handle-drag
                                             offset handle-id lock?)) db)))

(defmethod tool.hierarchy/on-drag [::edit/edit :edit]
  [db e]
  (let [{:keys [shift-key ctrl-key]} e
        lock? (or ctrl-key (input.handlers/multi-touch? db))
        {:keys [id parent]} (:clicked-element db)
        db (history.handlers/reset-state db)
        handle-selected? (element.handlers/handle-selected? db parent id)
        db (cond-> (history.handlers/reset-state db)
             (not handle-selected?)
             (element.handlers/toggle-handle-selection parent id
                                                       shift-key))
        offset (matrix/add (tool.handlers/pointer-delta db)
                           (snap.handlers/nearest-delta db))]
    (->> (element.handlers/selected db)
         (reduce (rpartial update-element offset lock?) db))))

(defmethod tool.hierarchy/on-drag-end [::edit/edit :edit]
  [db e]
  (-> db
      (tool.handlers/set-state :idle)
      (dissoc :clicked-element)
      (history.handlers/finalize (:timestamp e) [::edit/label "Edit"])))

(defmethod tool.hierarchy/snapping-points [::edit/edit :edit]
  [db]
  (when-let [{:keys [position label]
              :as el} (:clicked-element db)]
    [(with-meta
       (matrix/add position (tool.handlers/pointer-delta db))
       {:label (when (= (:type el) :handle)
                 (or label [::handle "handle"]))})]))

(defmethod tool.hierarchy/snapping-elements [::edit/edit :edit]
  [db]
  (element.handlers/non-selected-visible db))
