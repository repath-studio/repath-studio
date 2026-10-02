(ns renderer.element.impl.text-content.text-path
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/textPath"
  (:require
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.hierarchy :as hierarchy]))

(hierarchy/derive! :textPath ::element.hierarchy/text-content-child)

(defmethod element.hierarchy/properties :textPath
  []
  {:label [::label "Text path"]
   :description [::description
                 "The <textPath> SVG element is used to render text along the
                  shape of a <path> element. The text must be enclosed in the
                  <textPath> element and its href attribute is used to reference
                  the desired <path>."]})
