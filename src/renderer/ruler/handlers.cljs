(ns renderer.ruler.handlers
  (:require
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

(def major-sections 10)

(m/=> render-subgrid? [:-> number? GridStep Orientation boolean?])
(defn render-subgrid?
  [zoom grid-step orientation]
  (let [min-step (axis-min-step grid-step orientation)]
    (> (current-step zoom grid-step orientation)
       (step conf/grid-target-px conf/max-zoom min-step))))

(m/=> steps-coll [:-> number? Viewbox Orientation [:sequential number?]])
(defn steps-coll
  "Returns a collection of steps contained on the given viewbox."
  [ruler-step viewbox orientation]
  (let [[x y width height] viewbox
        vertical (= orientation :vertical)]
    (range (- (+ (* major-sections ruler-step)
                 (rem (if vertical y x)
                      (* major-sections ruler-step))))
           (if vertical height width)
           ruler-step)))

(m/=> axis-steps [:-> App [:enum :x :y] [:sequential number?]])
(defn axis-steps
  [db axis]
  (let [zoom (get-in db [:documents (:active-document db) :zoom])
        viewbox (frame.handlers/viewbox db)
        grid-step (get-in db [:documents (:active-document db)
                              :attrs :grid-step])
        horizontal? (= axis :x)
        orientation (if horizontal? :horizontal :vertical)
        origin (if horizontal? (first viewbox) (second viewbox))
        rendered-step (axis-step conf/grid-target-px zoom grid-step orientation)
        snap-step (if (render-subgrid? zoom grid-step orientation)
                    rendered-step
                    (* major-sections rendered-step))]
    (map #(+ % origin) (steps-coll snap-step viewbox orientation))))
