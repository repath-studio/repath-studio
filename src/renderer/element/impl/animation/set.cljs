(ns renderer.element.impl.animation.set
  "https://svgwg.org/specs/animations/#SetElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/set"
  (:require
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.hierarchy :as hierarchy]))

(hierarchy/derive! :set ::element.hierarchy/animation)

(defmethod element.hierarchy/properties :set
  []
  {:icon "animation"
   :label [::label "Set"]
   :permitted-content #{::element.hierarchy/descriptive}
   :description [::description
                 "The <set> SVG element provides a method of setting the value
                  of an attribute for a specified duration."]})
