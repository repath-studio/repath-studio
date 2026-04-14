(ns renderer.ruler.views
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.subs :as-alias element.subs]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.ruler.subs :as-alias ruler.subs]
   [renderer.window.subs :as-alias window.subs]))

(def ruler-size 24)

(defn bbox-rect
  [orientation]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        pan @(rf/subscribe [::document.subs/pan])
        bbox @(rf/subscribe [::element.subs/bbox])
        [min-x min-y max-x max-y] (map #(* % zoom) bbox)
        attrs (if (= orientation :vertical)
                {:x 0
                 :y (- min-y (* (second pan) zoom))
                 :width ruler-size
                 :height (- max-y min-y)}
                {:x (- min-x (* (first pan) zoom))
                 :y 0
                 :width (- max-x min-x)
                 :height ruler-size})]
    [:rect (merge attrs {:fill "var(--overlay)"})]))

(defn pointer
  [orientation]
  (let [[x y] @(rf/subscribe [::app.subs/pointer-pos])
        pointer-size (/ ruler-size 5)
        color "var(--color-accent)"
        vertical (= orientation :vertical)]
    [:g
     [:polygon {:fill color
                :points (string/join " " (if vertical
                                           [pointer-size "," y
                                            0 "," (- y pointer-size)
                                            0 "," (+ y pointer-size)]
                                           [x "," pointer-size
                                            (- x pointer-size) "," 0
                                            (+ x pointer-size) "," 0]))}]
     [:line (if vertical
              {:x1 0
               :y1 y
               :x2 ruler-size
               :y2 y
               :stroke color}
              {:x1 x
               :y1 0
               :x2 x
               :y2 ruler-size
               :stroke color})]]))

(defn line
  [{:keys [orientation adjusted-step size starting-point]}]
  [:line (if (= orientation :vertical)
           {:x1 starting-point
            :y1 adjusted-step
            :x2 size
            :y2 adjusted-step
            :stroke "var(--foreground-muted)"}
           {:x1 adjusted-step
            :y1 starting-point
            :x2 adjusted-step
            :y2 size
            :stroke "var(--foreground-muted)"})])

(defn label
  [orientation step font-size text]
  (let [x-step (+ step 4)
        y-step (- step 8)
        vertical (= orientation :vertical)]
    [:text {:x (if vertical 19 x-step)
            :y (if vertical y-step (inc font-size))
            :writing-mode (when vertical "vertical-rl")
            :fill "var(--foreground-default)"
            :font-size font-size
            :rotate (when vertical 180)
            :font-family "var(--font-mono)"}
     (if vertical (reverse text) text)]))

(defn base-lines
  [orientation]
  (let [[x y] @(rf/subscribe [::frame.subs/viewbox])
        zoom @(rf/subscribe [::document.subs/zoom])
        steps-coll @(rf/subscribe [::ruler.subs/steps-coll orientation])
        vertical (= orientation :vertical)]
    (into [:g]
          (map-indexed
           (fn [index step]
             (let [adjusted-step (* zoom step)
                   font-size 9
                   text (-> step
                            (+ (if vertical y x))
                            (Math/round)
                            (str))]
               (cond
                 (zero? (rem index 10))
                 [:<>
                  [line {:orientation orientation
                         :adjusted-step adjusted-step
                         :size ruler-size
                         :starting-point 0}]
                  [label orientation adjusted-step font-size text]]

                 (and (odd? index) (zero? (rem index 5)))
                 [line {:orientation orientation
                        :adjusted-step adjusted-step
                        :size ruler-size
                        :starting-point (/ ruler-size 1.6)}]

                 :else
                 [line {:orientation orientation
                        :adjusted-step adjusted-step
                        :size ruler-size
                        :starting-point (/ ruler-size 1.3)}])))
           steps-coll))))

(defn ruler
  [orientation]
  (let [vertical (= orientation :vertical)
        md? @(rf/subscribe [::window.subs/md?])]
    [:svg {:width (if vertical ruler-size "100%")
           :height (if vertical "100%" ruler-size)}
     (when md? [bbox-rect orientation])
     [base-lines orientation]
     (when md? [pointer orientation])]))

(defn grid-line
  [i step stroke-width orientation]
  (let [[x y w h] @(rf/subscribe [::frame.subs/viewbox-bounds])
        vertical (= orientation :vertical)
        step-x (+ step x)
        step-y (+ step y)
        main? (zero? (rem i 10))]
    [:line {:x1 (if vertical x step-x)
            :y1 (if vertical step-y y)
            :x2 (if vertical w step-x)
            :y2 (if vertical step-y h)
            :stroke-width stroke-width
            :opacity (when-not main? ".5")
            :stroke "var(--border)"
            :pointer-events "none"}]))

(defn grid-plane
  [orientation]
  (let [coll @(rf/subscribe [::ruler.subs/steps-coll orientation])
        zoom @(rf/subscribe [::document.subs/zoom])
        subgrid? @(rf/subscribe [::ruler.subs/subgrid?])
        stroke-width (/ 1 zoom)
        render-line (fn [i step]
                      (when (or (zero? (rem i 10)) subgrid?)
                        (grid-line i step stroke-width orientation)))]
    (->> coll
         (map-indexed render-line)
         (into [:g]))))

(defn grid
  []
  [:<>
   [grid-plane :vertical]
   [grid-plane :horizontal]])
