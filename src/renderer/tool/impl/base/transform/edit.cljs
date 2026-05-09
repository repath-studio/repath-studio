(ns renderer.tool.impl.base.transform.edit
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [BBox Orientation Vec2]]
   [renderer.element.handlers :as element.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.transform.core :as-alias transform]
   [renderer.tool.impl.base.transform.translate :as transform.translate]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.views :as views]))

(defmethod tool.hierarchy/help [::transform/transform :edit]
  []
  (i18n.views/t [::help-edit "Hold %1 to restrict direction."]
                [[views/kbd "Ctrl"]]))

(m/=> translate-offset [:-> App Vec2 [:maybe Orientation] BBox App])
(defn translate-offset
  [db delta axis bbox]
  (let [[delta-x delta-y] delta
        [w h] (utils.bounds/->dimensions bbox)
        factor-delta (case axis
                       :vertical [(/ delta-x w) 0]
                       :horizontal [0 (/ delta-y h)]
                       [(/ delta-x w) (/ delta-y h)])]
    (update db :anchor-offset matrix/add factor-delta)))

(defmethod tool.hierarchy/on-drag [::transform/transform :edit]
  [db e]
  (let [bbox (element.handlers/bbox db)
        delta (tool.handlers/pointer-delta db)
        axis (when (:ctrl-key e) (transform.translate/direction delta))]
    (-> db
        (assoc :anchor-offset (:anchor-point db))
        (translate-offset delta axis bbox)
        (snap.handlers/snap-with translate-offset axis bbox))))

(defmethod tool.hierarchy/on-drag-end [::transform/transform :edit]
  [db _e]
  (-> db
      (assoc :anchor-point (:anchor-offset db))
      (tool.handlers/set-state :idle)
      (dissoc :clicked-element :pivot-point)))

(defmethod tool.hierarchy/snapping-points [::transform/transform :edit]
  [db]
  (when-let [el (:clicked-element db)]
    [(with-meta
       (matrix/add [(:x el) (:y el)]
                   (tool.handlers/pointer-delta db))
       {:label [::pivot-handle "pivot handle"]})]))

(defmethod tool.hierarchy/snapping-elements [::transform/transform :edit]
  [db]
  (element.handlers/selected db))
