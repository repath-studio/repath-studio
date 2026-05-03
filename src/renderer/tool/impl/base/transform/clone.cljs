(ns renderer.tool.impl.base.transform.clone
  (:require
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.transform.core :as-alias transform]
   [renderer.tool.impl.base.transform.select :as transform.select]
   [renderer.tool.impl.base.transform.translate :as transform.translate]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.views :as views]))

(defmethod tool.hierarchy/help [::transform/transform :clone]
  []
  (i18n.views/t [::clone
                 [:div "Hold %1 to restrict direction. or release %2 to move"]]
                [[views/kbd "Ctrl"]
                 [views/kbd "Alt"]]))

(defmethod tool.hierarchy/on-drag [::transform/transform :clone]
  [db e]
  (let [{:keys [ctrl-key shift-key alt-key]} e
        delta (tool.handlers/pointer-delta db)]
    (if alt-key
      (-> db
          (history.handlers/reset-state)
          (transform.select/select-element shift-key)
          (element.handlers/duplicate)
          (transform.translate/translate delta ctrl-key)
          (snap.handlers/snap-with transform.translate/translate ctrl-key)
          (tool.handlers/set-cursor "copy"))
      (tool.handlers/set-state db :translate))))

(defmethod tool.hierarchy/on-drag-end [::transform/transform :clone]
  [db e]
  (-> db
      (tool.handlers/set-state :idle)
      (dissoc :clicked-element :pivot-point)
      (history.handlers/finalize (:timestamp e)
                                 [::clone-selection "Clone selection"])))

(defmethod tool.hierarchy/snapping-points [::transform/transform :clone]
  [db]
  (let [selected (element.handlers/selected db)
        options (-> db :snap :options)]
    (cond-> (element.handlers/snapping-points db (filter :visible selected))
      (seq (rest selected))
      (into (utils.bounds/->snapping-points (element.handlers/bbox db)
                                            options)))))

(defmethod tool.hierarchy/snapping-elements [::transform/transform :clone]
  [db]
  (element.handlers/non-selected-visible db))
