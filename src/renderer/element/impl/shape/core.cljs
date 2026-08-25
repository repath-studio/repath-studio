(ns renderer.element.impl.shape.core
  "https://www.w3.org/TR/SVG/shapes.html#TermShapeElement"
  (:require
   [re-frame.core :as rf]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.impl.shape.circle]
   [renderer.element.impl.shape.ellipse]
   [renderer.element.impl.shape.image]
   [renderer.element.impl.shape.line]
   [renderer.element.impl.shape.path]
   [renderer.element.impl.shape.poly]
   [renderer.element.impl.shape.polygon]
   [renderer.element.impl.shape.polyline]
   [renderer.element.impl.shape.rect]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.hierarchy :as hierarchy]
   [renderer.tool.subs :as-alias tool.subs]))

(hierarchy/derive! ::element.hierarchy/shape ::element.hierarchy/graphics)

(defmethod element.hierarchy/permitted-content ::element.hierarchy/shape
  [_el]
  #{::element.hierarchy/animation
    ::element.hierarchy/descriptive})

(defmethod element.hierarchy/render ::element.hierarchy/shape
  [el]
  (let [child-els @(rf/subscribe [::element.subs/filter-visible (:children el)])
        idle? @(rf/subscribe [::tool.subs/idle?])]
    [element.views/render-to-dom el child-els idle?]))
