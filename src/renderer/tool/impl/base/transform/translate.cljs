(ns renderer.tool.impl.base.transform.translate
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [Orientation Vec2]]
   [renderer.element.db :refer [Element ElementId]]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.input.handlers :as input.handlers]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.transform.core :as-alias transform]
   [renderer.tool.impl.base.transform.select :as transform.select]
   [renderer.tool.views :as tool.views]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.extra :refer [rpartial]]
   [renderer.utils.math :as utils.math]
   [renderer.utils.transform :as utils.transform]
   [renderer.views :as views]))

(defmethod tool.hierarchy/help [::transform/transform :translate]
  []
  (i18n.views/t [::translate
                 [:div "Hold %1 to restrict direction, and %2 to clone."]]
                [[tool.views/primary-mod-key]
                 [views/kbd "Alt"]]))

(m/=> swap-parent [:-> App ElementId Element App])
(defn swap-parent
  [db id hovered-svg]
  (let [{:keys [local-pointer-pos]} db
        transform (element.handlers/transform db id)
        db (element.handlers/set-parent db id (:id hovered-svg))
        updated-transform (element.handlers/transform db id)
        landed (->> local-pointer-pos
                    (utils.element/untransform-point transform)
                    (utils.transform/point updated-transform))
        offset (utils.math/v-sub local-pointer-pos landed)]
    (element.handlers/translate db id offset)))

(m/=> translate-el [:-> App ElementId map? App])
(defn translate-el
  [db id {:keys [offset hovered-svg auto-parent]}]
  (let [el (element.handlers/entity db id)]
    (cond-> db
      :always
      (element.handlers/translate id offset)

      (and auto-parent
           (not (utils.element/svg? el))
           (not (utils.element/top-level? el)))
      (swap-parent id hovered-svg))))

(m/=> direction [:-> Vec2 Orientation])
(defn direction
  [delta]
  (let [[delta-x delta-y] delta]
    (if (> (abs delta-x) (abs delta-y))
      :horizontal
      :vertical)))

(m/=> translate [:-> App Vec2 boolean? App])
(defn translate
  [db offset lock-direction?]
  (let [[offset-x offset-y] offset
        axis (when lock-direction? (direction offset))
        hovered-svg (element.handlers/hovered-svg db)
        top-ancestor-ids (element.handlers/top-ancestor-sorted-ids db)
        parent-ids (element.handlers/parent-ids db)
        parent-el (element.handlers/entity db (first parent-ids))
        auto-parent? (and (contains? #{:translate :clone} (:state db))
                          (not= (:id parent-el) (:id hovered-svg))
                          (not (next parent-ids))
                          (or (utils.element/svg? parent-el)
                              (utils.element/root? parent-el)))
        offset (case axis
                 :horizontal [offset-x 0]
                 :vertical [0 offset-y]
                 offset)]
    (->> top-ancestor-ids
         (reduce (rpartial translate-el {:offset offset
                                         :hovered-svg hovered-svg
                                         :auto-parent auto-parent?}) db))))

(defmethod tool.hierarchy/on-drag [::transform/transform :translate]
  [db e]
  (let [{:keys [shift-key alt-key]} e
        delta (tool.handlers/pointer-delta db)
        selected-elements (element.handlers/selected db)
        locked? (every? :locked selected-elements)
        primary-mod? (input.handlers/primary-mod? db e)
        multi-touch? (input.handlers/multi-touch? db)]
    (if alt-key
      (tool.handlers/set-state db :clone)
      (-> db
          (history.handlers/reset-state)
          (transform.select/select-element shift-key)
          (translate delta (or primary-mod? multi-touch?))
          (snap.handlers/snap-with translate primary-mod?)
          (tool.handlers/set-cursor (if locked? "not-allowed" "move"))))))

(defmethod tool.hierarchy/on-drag-end [::transform/transform :translate]
  [db e]
  (-> db
      (tool.handlers/set-state :idle)
      (dissoc :clicked-element :pivot-point)
      (history.handlers/finalize (:timestamp e)
                                 [::move-selection "Move selection"])))

(defmethod tool.hierarchy/snapping-points [::transform/transform :translate]
  [db]
  (let [selected (element.handlers/selected db)
        options (-> db :snap :options)]
    (cond
      (not= (:state db) :idle)
      (cond-> (element.handlers/snapping-points db (filter :visible selected))
        (seq (rest selected))
        (into (utils.bounds/->snapping-points (element.handlers/bbox db)
                                              options))))))

(defmethod tool.hierarchy/snapping-elements [::transform/transform :translate]
  [db]
  (element.handlers/non-selected-visible db))
