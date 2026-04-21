(ns renderer.element.impl.renderable
  "https://www.w3.org/TR/SVG/render.html#TermRenderableElement"
  (:require
   [re-frame.core :as rf]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.tool.subs :as-alias tool.subs]))

(element.hierarchy/derive-element
 ::element.hierarchy/renderable
 ::element.hierarchy/element)

(defmethod element.hierarchy/render ::element.hierarchy/renderable
  [el]
  (let [child-els @(rf/subscribe [::element.subs/filter-visible (:children el)])
        idle? @(rf/subscribe [::tool.subs/idle?])]
    [element.views/render-to-dom el child-els idle?]))
