(ns renderer.element.impl.text-content.text
  "https://www.w3.org/TR/SVG/text.html
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/text"
  (:require
   [re-frame.core :as rf]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.hierarchy :as hierarchy]
   [renderer.tool.subs :as-alias tool.subs]))

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

(defmethod element.hierarchy/render ::element.hierarchy/text-content
  [el]
  (let [child-els @(rf/subscribe [::element.subs/filter-visible (:children el)])
        idle? @(rf/subscribe [::tool.subs/idle?])
        editing? @(rf/subscribe [::tool.subs/editing?])]
    (when-not (and editing? (:selected el))
      [:g {:cursor (when editing? "text")}
       [element.views/render-to-dom el child-els idle?]])))
