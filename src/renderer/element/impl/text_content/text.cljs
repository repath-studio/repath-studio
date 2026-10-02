(ns renderer.element.impl.text-content.text
  "https://www.w3.org/TR/SVG/text.html
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/text"
  (:require
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.hierarchy :as hierarchy]))

(hierarchy/derive! :text ::element.hierarchy/text-content)

(defmethod element.hierarchy/properties :text
  []
  {:icon "text"
   :label [::label "Text"]
   :description [::description
                 "The SVG <text> element draws a graphics element consisting
                  of text. It's possible to apply a gradient, pattern,
                  clipping path, mask, or filter to <text>, like any other
                  SVG graphics element."]
   :ratio-locked true
   :attrs [:font-family
           :font-size
           :font-weight
           :font-style
           :stroke
           :stroke-width
           :stroke-dasharray
           :opacity]})
