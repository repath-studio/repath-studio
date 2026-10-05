(ns renderer.element.impl.text-content.tspan
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/tspan"
  (:require
   [re-frame.core :as rf]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.hierarchy :as hierarchy]
   [renderer.tool.subs :as-alias tool.subs]))

(hierarchy/derive! :tspan ::element.hierarchy/text-content-child)

(defmethod element.hierarchy/properties :tspan
  []
  {:label [::label "TSpan"]
   :description [::description
                 "The <tspan> SVG element defines a subtext within a <text>
                  element or another <tspan> element. It allows for adjustment
                  of the style and/or position of that subtext as needed."]})

(defmethod element.hierarchy/render :tspan
  [el]
  (let [child-els @(rf/subscribe [::element.subs/filter-visible (:children el)])
        idle? @(rf/subscribe [::tool.subs/idle?])]
    [element.views/render-to-dom el child-els idle?]))
