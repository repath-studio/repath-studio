(ns renderer.tool.impl.base.transform.translate
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [Orientation Vec2]]
   [renderer.element.db :refer [Element ElementId]]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.transform.core :as-alias transform]
   [renderer.tool.impl.base.transform.select :as transform.select]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.extra :refer [rpartial]]
   [renderer.views :as views]))

(defmethod tool.hierarchy/help [::transform/transform :translate]
  []
  (i18n.views/t [::translate
                 [:div "Hold %1 to restrict direction, and %2 to clone."]]
                [[views/kbd "Ctrl"]
                 [views/kbd "Alt"]]))

(m/=> start-point [:-> Element Vec2])
(defn start-point
  [el]
  (into [] (take 2) (:bbox el)))

(m/=> swap-parent [:-> App ElementId Element Element App])
(defn swap-parent
  [db id hovered-svg container-el]
  (cond-> db
    :always
    (element.handlers/set-parent id (:id hovered-svg))

    (:bbox container-el)
    (element.handlers/translate id (start-point container-el))

    (:bbox hovered-svg)
    (element.handlers/translate id (matrix/mul (start-point hovered-svg) -1))))

(m/=> translate-el [:-> App ElementId map? App])
(defn translate-el
  [db id {:keys [offset hovered-svg auto-parent]}]
  (let [el (element.handlers/entity db id)
        container-el (element.handlers/parent-container db id)]
    (cond-> db
      :always
      (element.handlers/translate id offset)

      (and auto-parent
           (not (utils.element/svg? el))
           (not (utils.element/top-level? el)))
      (swap-parent id hovered-svg container-el))))

(m/=> direction [:-> Vec2 Orientation])
(defn direction
  [delta]
  (let [[delta-x delta-y] delta]
    (if (> (abs delta-x) (abs delta-y))
      :vertical
      :horizontal)))

(m/=> translate [:-> App Vec2 boolean? App])
(defn translate
  [db offset lock-direction?]
  (let [[offset-x offset-y] offset
        axis (when lock-direction? (direction offset))
        hovered-svg (element.handlers/hovered-svg db)
        top-ancestor-ids (element.handlers/top-ancestor-ids db)
        parent-ids (element.handlers/parent-ids db)
        parent-el (element.handlers/entity db (first parent-ids))
        auto-parent? (and (contains? #{:translate :clone} (:state db))
                          (not= (:id parent-el) (:id hovered-svg))
                          (not (next parent-ids))
                          (utils.element/container? parent-el))
        offset (case axis
                 :vertical [offset-x 0]
                 :horizontal [0 offset-y]
                 offset)]
    (->> top-ancestor-ids
         (reduce (rpartial translate-el {:offset offset
                                         :hovered-svg hovered-svg
                                         :auto-parent auto-parent?}) db))))

(defmethod tool.hierarchy/on-drag [::transform/transform :translate]
  [db e]
  (let [{:keys [shift-key ctrl-key alt-key]} e
        delta (tool.handlers/pointer-delta db)
        selected-elements (element.handlers/selected db)
        locked? (every? :locked selected-elements)]
    (if alt-key
      (tool.handlers/set-state db :clone)
      (-> db
          (history.handlers/reset-state)
          (transform.select/select-element shift-key)
          (translate delta ctrl-key)
          (snap.handlers/snap-with translate ctrl-key)
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
