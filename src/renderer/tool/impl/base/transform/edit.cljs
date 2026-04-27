(ns renderer.tool.impl.base.transform.edit
  (:require [clojure.core.matrix :as matrix]
            [malli.core :as m]
            [renderer.app.db :refer [App]]
            [renderer.db :refer [BBox Orientation Vec2]]
            [renderer.element.handlers :as element.handlers]
            [renderer.i18n.views :as i18n.views]
            [renderer.snap.handlers :as snap.handlers]
            [renderer.tool.hierarchy :as tool.hierarchy]
            [renderer.utils.bounds :as utils.bounds]
            [renderer.views :as views]))

(defmethod tool.hierarchy/help [:transform :edit]
  []
  (i18n.views/t [::help-edit "Hold %1 to restrict direction."]
                [[views/kbd "Ctrl"]]))

(m/=> translate-offset [:-> App Vec2 Orientation BBox App])
(defn translate-offset
  [db delta axis bbox]
  (let [[delta-x delta-y] delta
        [w h] (utils.bounds/->dimensions bbox)
        factor-delta (case axis
                       :vertical [(/ delta-x w) 0]
                       :horizontal [0 (/ delta-y h)]
                       [(/ delta-x w) (/ delta-y h)])]
    (update db :anchor-offset matrix/add factor-delta)))

(m/=> on-drag [:-> App Vec2 Orientation App])
(defn on-drag
  [db delta axis]
  (let [bbox (element.handlers/bbox db)]
    (-> db
        (assoc :anchor-offset (:anchor-point db))
        (translate-offset delta axis bbox)
        (snap.handlers/snap-with translate-offset axis bbox))))
