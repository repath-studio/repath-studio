(ns renderer.tool.impl.base.transform.edit
  (:require [clojure.core.matrix :as matrix]
            [malli.core :as m]
            [renderer.app.db :refer [App]]
            [renderer.db :refer [Orientation Vec2]]
            [renderer.i18n.views :as i18n.views]
            [renderer.snap.handlers :as snap.handlers]
            [renderer.tool.hierarchy :as tool.hierarchy]
            [renderer.views :as views]))

(defmethod tool.hierarchy/help [:transform :edit]
  []
  (i18n.views/t [::help-edit "Hold %1 to restrict direction."]
                [[views/kbd "Ctrl"]]))

(m/=> translate-offset [:-> App Vec2 Orientation App])
(defn translate-offset
  [db delta axis]
  (let [[delta-x delta-y] delta]
    (update db :anchor-offset matrix/add (case axis
                                           :vertical [delta-x 0]
                                           :horizontal [0 delta-y]
                                           delta))))

(m/=> on-drag [:-> App Vec2 Orientation App])
(defn on-drag
  [db delta axis]
  (-> db
      (assoc :anchor-offset (:anchor-point db))
      (translate-offset delta axis)
      (snap.handlers/snap-with translate-offset axis)))
