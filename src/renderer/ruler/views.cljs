(ns renderer.ruler.views
  (:require
   [clojure.math.combinatorics :as math.combinatorics]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.subs :as-alias element.subs]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.input.subs :as-alias input.subs]
   [renderer.ruler.handlers :as ruler.handlers]
   [renderer.ruler.subs :as-alias ruler.subs]
   [renderer.tool.events :as tool.events]
   [renderer.tool.impl.misc.guide :as-alias tool.impl.misc.guide]
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
  (let [[x y] @(rf/subscribe [::input.subs/pointer-pos])
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
        subgrid? @(rf/subscribe [::ruler.subs/render-subgrid? orientation])
        active? @(rf/subscribe [::tool.subs/active?
                                ::tool.impl.misc.guide/guide])
        vertical (= orientation :vertical)]
    (into [:g]
          (keep-indexed
           (fn [index step]
             (let [adjusted-step (* zoom step)
                   major? (zero? (rem index ruler.handlers/major-sections))
                   starting-point (if major? 0 (/ ruler-size 1.3))
                   line [ruler-line {:orientation orientation
                                     :adjusted-step adjusted-step
                                     :size ruler-size
                                     :starting-point starting-point
                                     :active active?}]
                   text (-> step
                            (+ (if vertical y x))
                            (Math/round)
                            (str))]
               (if major?
                 [:<> line [label active? orientation adjusted-step text]]
                 (when subgrid? line))))
           steps-coll))))

(defn ruler
  [orientation]
  (let [vertical (= orientation :vertical)
        md? @(rf/subscribe [::window.subs/md?])]
    [:svg
     {:width (if vertical ruler-size "100%")
      :height (if vertical "100%" ruler-size)
      :cursor (if vertical "col-resize" "row-resize")
      :on-pointer-down (fn [e]
                         ;; Prevent action when if the event was propagated
                         ;; through a panel separator.
                         (when-not (.-defaultPrevented e)
                           (rf/dispatch [::tool.events/activate
                                         ::tool.impl.misc.guide/guide
                                         :orientation orientation])))}
     (when md? [bbox-rect orientation])
     [base-lines orientation]
     (when md? [pointer orientation])]))

(defn axis-params
  [orientation]
  (let [major-sections ruler.handlers/major-sections
        step @(rf/subscribe [::ruler.subs/step orientation])
        subgrid? @(rf/subscribe [::ruler.subs/render-subgrid? orientation])
        opacity @(rf/subscribe [::ruler.subs/subgrid-opacity orientation])
        size (* major-sections step)
        offset (/ step 2)
        major-index (quot major-sections 2)]
    {:size size
     :origin (- (+ (/ size 2) offset))
     :major-index major-index
     :indexes (if subgrid? (range major-sections) [major-index])
     :position (fn [index] (+ (* index step) offset))
     :opacity opacity}))

(defn axis-lines
  [{:keys [position indexes major-index opacity]} span vertical? unit]
  (map (fn [index]
         (let [attrs (if vertical?
                       {:x1 (position index)
                        :y1 0
                        :x2 (position index)
                        :y2 span}
                       {:x1 0
                        :y1 (position index)
                        :x2 span
                        :y2 (position index)})]
           [:line (cond-> (assoc attrs
                                 :stroke "var(--border)"
                                 :stroke-width unit)
                    (not= index major-index)
                    (assoc :opacity opacity))]))
       indexes))

(def grid-id "canvas-grid")

(defn axis-dots
  [x-axis y-axis unit]
  (let [{:keys [position indexes major-index opacity]} x-axis]
    (map (fn [[x-index y-index]]
           (let [minor? (or (not= x-index major-index)
                            (not= y-index (:major-index y-axis)))]
             [:circle (cond-> {:cx (position x-index)
                               :cy ((:position y-axis) y-index)
                               :fill "var(--dot)"
                               :r (* 1.5 unit)}
                        minor?
                        (assoc :opacity (min opacity (:opacity y-axis))))]))
         (math.combinatorics/cartesian-product indexes (:indexes y-axis)))))

(defn grid-pattern
  []
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        dots? @(rf/subscribe [::document.subs/grid-style? :dots])
        x-axis (axis-params :horizontal)
        y-axis (axis-params :vertical)
        unit (/ 1 zoom)]
    (into [:pattern {:id grid-id
                     :patternUnits "userSpaceOnUse"
                     :width (:size x-axis)
                     :height (:size y-axis)
                     :x (:origin x-axis)
                     :y (:origin y-axis)}]
          (if dots?
            (axis-dots x-axis y-axis unit)
            (concat (axis-lines x-axis (:size y-axis) true unit)
                    (axis-lines y-axis (:size x-axis) false unit))))))

(defn grid-plane
  []
  (let [[x y x2 y2] @(rf/subscribe [::frame.subs/viewbox-bounds])]
    [:rect {:x x
            :y y
            :width (- x2 x)
            :height (- y2 y)
            :fill (str "url(#" grid-id ")")
            :pointer-events "none"}]))

(defn grid
  []
  [:<>
   [grid-pattern]
   [grid-plane]])
