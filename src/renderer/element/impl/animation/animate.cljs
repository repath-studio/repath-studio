(ns renderer.element.impl.animation.animate
  "https://svgwg.org/specs/animations/#AnimateElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/animate"
  (:require
   [renderer.element.hierarchy :as element.hierarchy]))

(element.hierarchy/derive-element :animate ::element.hierarchy/animation)

(defmethod element.hierarchy/properties :animate
  []
  {:icon "animation"
   :label [::label "Animate"]
   :description [::description
                 "The SVG <animate> element provides a way to animate an
                  attribute of an element over time."]
   :attrs [:href
           :attributeName
           :begin
           :end
           :dur
           :min
           :max
           :restart
           :repeatCount
           :repeatDur
           :fill
           :calcMode
           :values
           :keyTimes
           :keySplines
           :from
           :to
           :by
           :autoReverse
           :accelerate
           :decelerate
           :additive
           :accumulate
           :id
           :class]})
