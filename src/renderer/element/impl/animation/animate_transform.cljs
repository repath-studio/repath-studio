(ns renderer.element.impl.animation.animate-transform
  "https://svgwg.org/specs/animations/#AnimateTransformElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/animateTransform"
  (:require
   [renderer.element.hierarchy :as element.hierarchy]))

(element.hierarchy/derive-element
 :animateTransform
 ::element.hierarchy/animation)

(defmethod element.hierarchy/properties :animateTransform
  []
  {:icon "animation"
   :label [::label "Animate Transform"]
   :description [::description
                 "The <animateTransform> element animates a
                  transformation attribute on its target element, thereby
                  allowing animations to control translation, scaling,
                  rotation, and/or skewing"]
   :attrs [:type
           :from
           :to
           :by]})
