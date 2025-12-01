(ns renderer.utils.svg
  "Render functions for canvas overlay objects."
  (:require
   ["react" :as react]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [BBox Vec2]]
   [renderer.document.subs :as-alias document.subs]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.math :as utils.math]))

(m/=> dot [:-> Vec2 any? any?])
(defn dot
  [[x y] & children]
  (let [zoom @(rf/subscribe [::document.subs/zoom])]
    (into [:circle {:cx x
                    :cy y
                    :stroke-width 0
                    :fill "var(--accent)"
                    :r (/ 3 zoom)}] children)))

(m/=> line [:-> Vec2 Vec2 any?])
(defn line
  [[x1 y1] [x2 y2] & {:keys [stroke
                             stroke-width
                             stroke-linecap
                             stroke-opacity
                             stroke-dasharray]
                      :or {stroke "var(--accent)"
                           stroke-width 1
                           stroke-opacity 1
                           stroke-linecap "round"}}]
  (let [zoom @(rf/subscribe [::document.subs/zoom])]
    [:line {:x1 x1
            :y1 y1
            :x2 x2
            :y2 y2
            :stroke stroke
            :stroke-opacity stroke-opacity
            :stroke-width (/ stroke-width zoom)
            :stroke-linecap stroke-linecap
            :stroke-dasharray (/ stroke-dasharray zoom)}]))

(m/=> cross [:-> Vec2 any?])
(defn cross
  [[x y]]
  (let [handle-size @(rf/subscribe [::document.subs/handle-size])
        mid (/ handle-size 2)
        start-a [(- x mid) y]
        end-a [(+ x mid) y]
        start-b [x (- y mid)]
        end-b [x (+ y mid)]
        line-bg-attrs {:stroke "var(--accent-foreground)"
                       :stroke-width 3
                       :stroke-opacity ".5"}]
    [:g
     [line start-a end-a line-bg-attrs]
     [line start-b end-b line-bg-attrs]
     [line start-a end-a]
     [line start-b end-b]]))

(m/=> arc [:-> Vec2 number? number? number? any?])
(defn arc
  [[x y] radius start-degrees size-degrees]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        radius (/ radius zoom)
        end-degrees (+ start-degrees size-degrees)
        x1 (+ x (utils.math/angle-dx start-degrees radius))
        y1 (+ y (utils.math/angle-dy start-degrees radius))
        x2 (+ x (utils.math/angle-dx end-degrees radius))
        y2 (+ y (utils.math/angle-dy end-degrees radius))
        d (str "M" x1 "," y1 " "
               "A" radius "," radius " 0 0,1 " x2 "," y2)
        attrs {:d d
               :fill "transparent"
               :stroke-width (/ 1 zoom)}]
    [:g
     [:path (merge {:stroke "var(--accent-foreground)"} attrs)]
     [:path (merge {:stroke "var(--accent)"
                    :stroke-dasharray (/ 5 zoom)} attrs)]]))

(m/=> times [:-> Vec2 any?])
(defn times
  [[x y]]
  (let [handle-size @(rf/subscribe [::document.subs/handle-size])
        mid (/ handle-size Math/PI)
        x-1 (- x mid)
        x-2 (+ x mid)
        y-1 (- y mid)
        y-2 (+ y mid)
        bg-attrs {:stroke "var(--accent-foreground)"
                  :stroke-opacity ".5"
                  :stroke-width 3}]
    [:g {:style {:pointer-events "none"}}
     [line [x-1 y-1] [x-2 y-2] bg-attrs]
     [line [x-2 y-1] [x-1 y-2] bg-attrs]
     [line [x-1 y-1] [x-2 y-2]]
     [line [x-2 y-1] [x-1 y-2]]]))

(m/=> label [:-> string? map? any?])
(defn label
  [text attrs]
  (let [{:keys [x y text-anchor font-size font-family]} attrs
        rect-ref (react/createRef)
        zoom @(rf/subscribe [::document.subs/zoom])
        text-anchor (or text-anchor "middle")
        font-size (/ (or font-size 10) zoom)
        font-family (or font-family "var(--mono)")
        padding (/ 8 zoom)
        stroke-width (/ 1 zoom)
        label-height (+ font-size padding)]
    [:g
     [:rect {:ref rect-ref
             :y (- y (/ label-height 2))
             :fill "var(--accent)"
             :stroke "var(--accent-foreground)"
             :stroke-width stroke-width
             :stroke-opacity ".5"
             :rx (/ 4 zoom)
             :height label-height} text]
     [:text {:ref (fn [this]
                    (when (and this rect-ref)
                      (let [rect-el (.-current rect-ref)
                            rect-width (+ (.-width (.getBBox this)) padding)]
                        (.setAttribute rect-el "width" rect-width)
                        (.setAttribute rect-el "x"
                                       (case text-anchor
                                         "start" (- x (/ padding 2))
                                         "middle" (- x (/ rect-width 2))
                                         "end" (- x rect-width (/ (- padding)
                                                                  2)))))))
             :fill "var(--accent-foreground)"
             :dominant-baseline "middle"
             :x x
             :y y
             :text-anchor text-anchor
             :font-family font-family
             :font-size font-size} text]]))

(m/=> bounding-box [:-> BBox boolean? any?])
(defn bounding-box
  [bbox dashed?]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        [min-x min-y] bbox
        [w h] (utils.bounds/->dimensions bbox)
        attrs {:x min-x
               :y min-y
               :width w
               :height h
               :shape-rendering "crispEdges"
               :fill "transparent"}]
    [:g {:style {:pointer-events "none"}}
     [:rect (merge attrs {:stroke "var(--accent-foreground)"
                          :stroke-opacity (when-not dashed? ".5")
                          :stroke-width (/ (if dashed? 1 2) zoom)})]
     [:rect (merge attrs {:stroke "var(--accent)"
                          :stroke-width (/ 1 zoom)
                          :stroke-dasharray (when dashed? (/ 5 zoom))})]]))

(m/=> select-box [:-> App any?])
(defn select-box
  [db]
  (let [zoom (get-in db [:documents (:active-document db) :zoom])
        [pos-x pos-y] (:adjusted-pointer-pos db)
        [offset-x offset-y] (:adjusted-pointer-offset db)]
    {:tag :rect
     :attrs {:x (min pos-x offset-x)
             :y (min pos-y offset-y)
             :width (abs (- pos-x offset-x))
             :height (abs (- pos-y offset-y))
             :shape-rendering "crispEdges"
             :fill-opacity ".1"
             :fill "var(--accent)"
             :stroke "var(--accent)"
             :stroke-opacity ".5"
             :stroke-width (/ 1 zoom)}}))
