(ns renderer.element.impl.text-content.core
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.impl.text-content.text]
   [renderer.element.impl.text-content.text-node]
   [renderer.element.impl.text-content.tspan]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.hierarchy :as hierarchy]
   [renderer.tool.subs :as-alias tool.subs]))

(hierarchy/derive! ::element.hierarchy/text-content ::element.hierarchy/shape)
(hierarchy/derive! ::element.hierarchy/text-content-child
                   ::element.hierarchy/text-content)

(defmethod element.hierarchy/permitted-content ::element.hierarchy/text-content
  [_el]
  #{::element.hierarchy/animation
    ::element.hierarchy/descriptive
    ::element.hierarchy/text-content-child
    :a})

(defmethod element.hierarchy/translate ::element.hierarchy/text-content
  [el [x y]]
  (-> el
      (attribute.hierarchy/update-attr :x + x)
      (attribute.hierarchy/update-attr :y + y)))

(defmethod element.hierarchy/render ::element.hierarchy/text-content
  [el]
  (let [child-els @(rf/subscribe [::element.subs/filter-visible (:children el)])
        idle? @(rf/subscribe [::tool.subs/idle?])]
    [element.views/render-to-dom el child-els idle?]))
