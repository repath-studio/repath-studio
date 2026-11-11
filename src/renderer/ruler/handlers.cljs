(ns renderer.ruler.handlers
  (:require
   [clojure.math.combinatorics :as combo]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [Orientation Viewbox]]
   [renderer.document.db :refer [ZoomFactor]]
   [renderer.frame.handlers :as frame.handlers]))

(m/=> step [:-> ZoomFactor number?])
(defn step
  [zoom]
  (Math/pow 10 (Math/ceil (Math/log10 (/ 10 zoom)))))

(m/=> steps-coll [:-> number? Viewbox Orientation [:sequential number?]])
(defn steps-coll
  "Returns a collection of steps contained on the given viewbox."
  [ruler-step viewbox orientation]
  (let [[x y width height] viewbox
        sections 10
        vertical (= orientation :vertical)]
    (range (- (+ (* sections ruler-step)
                 (rem (if vertical y x)
                      (* sections ruler-step))))
           (if vertical height width)
           ruler-step)))

(m/=> steps-intersections [:-> App [:sequential [:sequential number?]]])
(defn steps-intersections
  "Returns the intersection points of the rulers."
  [db]
  (let [zoom (get-in db [:documents (:active-document db) :zoom])
        viewbox (frame.handlers/viewbox db)
        ruler-step (step zoom)]
    (combo/cartesian-product (steps-coll ruler-step viewbox :vertical)
                             (steps-coll ruler-step viewbox :horizontal))))
