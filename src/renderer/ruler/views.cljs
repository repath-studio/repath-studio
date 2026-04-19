(ns renderer.ruler.views
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.subs :as-alias element.subs]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.input.impl.pointer :as input.impl.pointer]
   [renderer.ruler.subs :as-alias ruler.subs]
   [renderer.tool.subs :as-alias tool.subs]
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

(defn ruler-line
  [{:keys [active orientation adjusted-step size starting-point]}]
  (let [stroke (if active "var(--accent-foreground)" "var(--foreground-muted)")]
    [:line (if (= orientation :vertical)
             {:x1 starting-point
              :y1 adjusted-step
              :x2 size
              :y2 adjusted-step
              :stroke stroke}
             {:x1 adjusted-step
              :y1 starting-point
              :x2 adjusted-step
              :y2 size
              :stroke stroke})]))

(defn label
  [active? orientation step text]
  (let [font-size 9
        x-step (+ step 4)
        y-step (- step 8)
        vertical (= orientation :vertical)]
    [:text {:x (if vertical 19 x-step)
            :y (if vertical y-step (inc font-size))
            :writing-mode (when vertical "vertical-rl")
            :font-size font-size
            :rotate (when vertical 180)
            :font-family "var(--font-mono)"
            :fill (if active?
                    "var(--accent-foreground)"
                    "var(--foreground-default)")}
     (if vertical (reverse text) text)]))

(defn base-lines
  [orientation]
  (let [[x y] @(rf/subscribe [::frame.subs/viewbox])
        zoom @(rf/subscribe [::document.subs/zoom])
        steps-coll @(rf/subscribe [::ruler.subs/steps-coll orientation])
        active? @(rf/subscribe [::tool.subs/active? :guide])
        vertical (= orientation :vertical)]
    (into [:g]
          (map-indexed
           (fn [index step]
             (let [adjusted-step (* zoom step)
                   text (-> step
                            (+ (if vertical y x))
                            (Math/round)
                            (str))]
               (cond
                 (zero? (rem index 10))
                 [:<>
                  [ruler-line {:orientation orientation
                               :adjusted-step adjusted-step
                               :size ruler-size
                               :starting-point 0
                               :active active?}]
                  [label active? orientation adjusted-step text]]

                 (and (odd? index) (zero? (rem index 5)))
                 [ruler-line {:orientation orientation
                              :adjusted-step adjusted-step
                              :size ruler-size
                              :starting-point (/ ruler-size 1.6)
                              :active active?}]

                 :else
                 [ruler-line {:orientation orientation
                              :adjusted-step adjusted-step
                              :size ruler-size
                              :starting-point (/ ruler-size 1.3)
                              :active active?}])))
           steps-coll))))

(defn ruler
  [orientation]
  (let [vertical (= orientation :vertical)
        md? @(rf/subscribe [::window.subs/md?])
        active? @(rf/subscribe [::tool.subs/active? :guide])
        pointer-handler (partial input.impl.pointer/handler!
                                 {:type :guide
                                  :orientation orientation})]
    [:svg
     {:width (if vertical ruler-size "100%")
      :height (if vertical "100%" ruler-size)
      :class (when active? "bg-accent")
      :on-pointer-down pointer-handler}
     (when md? [bbox-rect orientation])
     [base-lines orientation]
     (when md? [pointer orientation])]))

(defn grid-line
  [step orientation & {:as attrs}]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        [x y w h] @(rf/subscribe [::frame.subs/viewbox-bounds])
        vertical (= orientation :vertical)
        step-x (+ step x)
        step-y (+ step y)]
    [:line (merge {:x1 (if vertical x step-x)
                   :y1 (if vertical step-y y)
                   :x2 (if vertical w step-x)
                   :y2 (if vertical step-y h)
                   :stroke-width (/ 1 zoom)
                   :stroke "var(--border)"
                   :pointer-events "none"}
                  attrs)]))

(defn grid-plane
  [orientation]
  (let [coll @(rf/subscribe [::ruler.subs/steps-coll orientation])
        subgrid? @(rf/subscribe [::ruler.subs/subgrid?])
        render-line (fn [i step]
                      (let [main? (zero? (rem i 10))
                            opacity (when-not main? ".5")]
                        (when (or main? subgrid?)
                          (grid-line step orientation :opacity opacity))))]
    (->> coll
         (map-indexed render-line)
         (into [:g]))))

(defn grid
  []
  [:<>
   [grid-plane :vertical]
   [grid-plane :horizontal]])
