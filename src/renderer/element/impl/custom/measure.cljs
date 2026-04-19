(ns renderer.element.impl.custom.measure
  (:require
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.utils.length :as utils.length]
   [renderer.utils.math :as utils.math]
   [renderer.utils.svg :as utils.svg]))

(element.hierarchy/derive-element :measure ::element.hierarchy/renderable)

(defmethod element.hierarchy/properties :measure
  []
  {:icon "ruler-triangle"
   :label [::label "Measure"]
   :description [::element-description
                 "The <measure> element is used to create measurements between
                  two points."]
   :attrs [:x1
           :y1
           :x2
           :y2]})

(defmethod element.hierarchy/render :measure
  [el]
  (let [{:keys [x1 x2 y1 y2]} (:attrs el)
        [adjacent opposite] (matrix/sub [x1 y1] [x2 y2])
        hypotenuse (Math/hypot adjacent opposite)
        [x1 y1 x2 y2] (map utils.length/unit->px [x1 y1 x2 y2])
        angle (utils.math/angle [x1 y1] [x2 y2])
        zoom @(rf/subscribe [::document.subs/zoom])
        straight? (< angle 180)
        straight-angle (if straight? angle (- angle 360))
        line-bg-attrs {:stroke "var(--accent-foreground)"
                       :stroke-opacity ".5"
                       :stroke-width 3}]
    [:g
     [utils.svg/arc [x1 y1] 20 (if straight? 0 angle) (abs straight-angle)]

     [utils.svg/line [x1 y1] [x2 y2] line-bg-attrs]
     [utils.svg/line [x1 y1] [(+ x1 (/ 30 zoom)) y1] line-bg-attrs]

     [utils.svg/line [x1 y1] [x2 y2]]
     [utils.svg/line [x1 y1] [(+ x1 (/ 30 zoom)) y1]]

     [utils.svg/cross [x1 y1]]
     [utils.svg/cross [x2 y2]]

     [utils.svg/label
      (str (utils.length/->fixed straight-angle 2 false) "°")
      {:x (+ x1 (/ 40 zoom))
       :y y1
       :text-anchor "start"}]

     [utils.svg/label
      (-> hypotenuse js/parseFloat (utils.length/->fixed 2 false))
      {:x (/ (+ x1 x2) 2)
       :y (/ (+ y1 y2) 2)}]]))

(defmethod element.hierarchy/render-to-string :guide [_el] nil)
