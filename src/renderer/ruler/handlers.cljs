(ns renderer.ruler.handlers
  (:require
   [clojure.math.combinatorics :as combo]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [Orientation]]
   [renderer.document.db :refer [ZoomFactor]]
   [renderer.frame.db :refer [Viewbox]]
   [renderer.frame.handlers :as frame.handlers]))

(m/=> step [:-> ZoomFactor number?])
(defn step
  "Returns the grid step given a zoom level."
  [zoom]
  (let [raw-step (/ 10 zoom)
        magnitude (->> (Math/abs raw-step)
                       (Math/log10)
                       (Math/floor)
                       (Math/pow 10))
        normalized (/ raw-step magnitude)]
    (* magnitude (condp >= normalized
                   1.5 1
                   3.5 2
                   7 5
                   10))))

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
