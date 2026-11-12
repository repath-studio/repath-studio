(ns renderer.ruler.views
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.subs :as-alias element.subs]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.input.subs :as-alias input.subs]
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
        subgrid? @(rf/subscribe [::ruler.subs/render-subgrid?])
        active? @(rf/subscribe [::tool.subs/active?
                                ::tool.impl.misc.guide/guide])
        vertical (= orientation :vertical)]
    (into [:g]
          (keep-indexed
           (fn [index step]
             (let [adjusted-step (* zoom step)
                   major? (zero? (rem index 10))
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

(defn grid-lines
  [position size style]
  [[:line (into {:x1 position
                 :y1 0
                 :x2 position
                 :y2 size}
                style)]
   [:line (into {:x1 0
                 :y1 position
                 :x2 size
                 :y2 position}
                style)]])

(def grid-id "canvas-grid")

(defn grid-pattern
  []
  (let [major-sections 10
        zoom @(rf/subscribe [::document.subs/zoom])
        subgrid? @(rf/subscribe [::ruler.subs/render-subgrid?])
        ruler-step @(rf/subscribe [::ruler.subs/step])
        subgrid-opacity @(rf/subscribe [::ruler.subs/subgrid-opacity])
        size (* major-sections ruler-step)
        offset (/ ruler-step 2)
        origin (- (+ (/ size 2) offset))
        major-index (quot major-sections 2)
        minor-indexes (remove #(= major-index %) (range major-sections))
        position (fn [index] (+ (* index ruler-step) offset))
        major-style {:stroke-width (/ 1 zoom)
                     :stroke "var(--border)"}
        minor-style (assoc major-style :opacity subgrid-opacity)]
    (into [:pattern {:id grid-id
                     :patternUnits "userSpaceOnUse"
                     :width size
                     :height size
                     :x origin
                     :y origin}]
          cat
          (cond-> [(grid-lines (position major-index) size major-style)]
            subgrid?
            (concat (map #(grid-lines (position %) size minor-style)
                         minor-indexes))))))

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
