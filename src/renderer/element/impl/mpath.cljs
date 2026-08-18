(ns renderer.element.impl.mpath
  "https://svgwg.org/specs/animations/#MPathElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/mpath"
  (:require
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.hierarchy :as hierarchy]))

(hierarchy/derive! :mpath ::element.hierarchy/descriptive)

(defmethod element.hierarchy/properties :mpath
  []
  {:icon "bezier-curve"
   :label [::label "Motion Path"]
   :description [::description
                 "The <mpath> SVG sub-element for the <animateMotion> element
                  provides the ability to reference an external <path> element
                  as the definition of a motion path."]})
