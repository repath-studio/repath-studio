(ns renderer.element.impl.text-content.tspan
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/tspan"
  (:require
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.hierarchy :as hierarchy]))

(hierarchy/derive! :tspan ::element.hierarchy/text-content-child)

(defmethod element.hierarchy/properties :tspan
  []
  {:label [::label "TSpan"]
   :description [::description
                 "The <tspan> SVG element defines a subtext within a <text>
                  element or another <tspan> element. It allows for adjustment
                  of the style and/or position of that subtext as needed."]})
