(ns renderer.ruler.handlers
  (:require
   [clojure.math.combinatorics :as math.combinatorics]
   [config :as conf]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [Orientation Viewbox]]
   [renderer.document.db :refer [GridStep ZoomFactor]]
   [renderer.frame.handlers :as frame.handlers]))

(m/=> step [:-> number? ZoomFactor pos-int? number?])
(defn step
  [target-px zoom major-min-step]
  (let [exp (Math/floor (Math/log10 major-min-step))
        mantissa (/ major-min-step (Math/pow 10 exp))
        auto-step (->> (/ target-px zoom)
                       (Math/log10)
                       (Math/ceil)
                       (Math/pow 10)
                       (* mantissa))]
    (max (/ major-min-step 10) auto-step)))

(m/=> axis-min-step [:-> GridStep Orientation pos-int?])
(defn axis-min-step
  [grid-step orientation]
  (get grid-step (if (= orientation :vertical) :y :x)))

(m/=> axis-step [:-> number? ZoomFactor GridStep Orientation number?])
(defn axis-step
  [target-px zoom grid-step orientation]
  (step target-px zoom (axis-min-step grid-step orientation)))

(m/=> current-step [:-> ZoomFactor GridStep Orientation number?])
(defn current-step
  [zoom grid-step orientation]
  (axis-step conf/grid-target-px zoom grid-step orientation))

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
        attrs (get-in db [:documents (:active-document db) :attrs])
        steps (fn [orientation]
                (-> conf/grid-target-px
                    (step zoom (axis-min-step (:grid-step attrs) orientation))
                    (steps-coll viewbox orientation)))]
    (math.combinatorics/cartesian-product
     (steps :vertical)
     (steps :horizontal))))
