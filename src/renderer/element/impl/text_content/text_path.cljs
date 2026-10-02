(ns renderer.element.impl.text-content.text-path
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/textPath"
  (:require
   [re-frame.core :as rf]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.hierarchy :as hierarchy]
   [renderer.tool.subs :as-alias tool.subs]))

(hierarchy/derive! :textPath ::element.hierarchy/text-content-child)

(defmethod element.hierarchy/properties :textPath
  []
  {:label [::label "Text path"]
   :description [::description
                 "The <textPath> SVG element is used to render text along the
                  shape of a <path> element. The text must be enclosed in the
                  <textPath> element and its href attribute is used to reference
                  the desired <path>."]})

(defmethod element.hierarchy/render :textPath
  [el]
  (let [child-els @(rf/subscribe [::element.subs/filter-visible (:children el)])
        idle? @(rf/subscribe [::tool.subs/idle?])]
    [element.views/render-to-dom el child-els idle?]))
