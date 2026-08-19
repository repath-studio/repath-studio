(ns renderer.element.impl.animation.animate
  "https://svgwg.org/specs/animations/#AnimateElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/animate"
  (:require
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.hierarchy :as hierarchy]))

(hierarchy/derive! :animate ::element.hierarchy/animation)

(defmethod element.hierarchy/properties :animate
  []
  {:icon "animation"
   :label [::label "Animate"]
   :permitted-content #{::element.hierarchy/descriptive}
   :description [::description
                 "The SVG <animate> element provides a way to animate an
                  attribute of an element over time."]
   :attrs [:begin
           :end
           :min
           :max
           :restart
           :repeatDur
           :calcMode
           :values
           :keyPoints
           :keyTimes
           :keySplines
           :by
           :additive
           :accumulate
           :id
           :class]})
