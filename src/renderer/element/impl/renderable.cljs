(ns renderer.element.impl.renderable
  "https://www.w3.org/TR/SVG/render.html#TermRenderableElement"
  (:require
   [re-frame.core :as rf]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.hierarchy :as hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.element :as utils.element]))

(hierarchy/derive! ::element.hierarchy/renderable ::element.hierarchy/element)

(defmethod element.hierarchy/render ::element.hierarchy/renderable
  [el]
  (let [child-els @(rf/subscribe [::element.subs/filter-visible (:children el)])
        idle? @(rf/subscribe [::tool.subs/idle?])]
    [element.views/render-to-dom el child-els idle?]))

(defmethod element.hierarchy/render-to-string ::element.hierarchy/renderable
  [el]
  (let [{:keys [tag attrs title children content]} el
        child-elements @(rf/subscribe [::element.subs/filter-visible children])
        attrs (->> (utils.element/style->map attrs)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (into [tag attrs (when title [:title title]) content]
          (map element.hierarchy/render-to-string child-elements))))

(defmethod element.hierarchy/bbox ::element.hierarchy/renderable
  [el]
  (:bbox (utils.element/get-computed-styles el)))
