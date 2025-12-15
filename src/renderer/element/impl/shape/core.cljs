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
   [renderer.utils.element :as utils.element]))

(element.hierarchy/derive-element
 ::element.hierarchy/shape
 ::element.hierarchy/graphics)

(defmethod element.hierarchy/render-to-string ::element.hierarchy/shape
  [el]
  (let [{:keys [tag attrs title children content]} el
        child-elements @(rf/subscribe [::element.subs/filter-visible children])
        attrs (->> (utils.element/style->map attrs)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (into [tag attrs (when title [:title title]) content]
          (map element.hierarchy/render-to-string child-elements))))
