(ns renderer.element.impl.shape.core
  "https://www.w3.org/TR/SVG/shapes.html#TermShapeElement"
  (:require
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
   [renderer.hierarchy :as hierarchy]))

(hierarchy/derive! ::element.hierarchy/shape ::element.hierarchy/graphics)

(defmethod element.hierarchy/permitted-content ::element.hierarchy/shape
  [_el]
  #{::element.hierarchy/animation
    ::element.hierarchy/descriptive})
