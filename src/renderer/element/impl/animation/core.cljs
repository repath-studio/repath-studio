(ns renderer.element.impl.animation.core
  "https://svgwg.org/specs/animations/#AnimationElements"
  (:require
   [re-frame.core :as rf]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.impl.animation.animate]
   [renderer.element.impl.animation.animate-motion]
   [renderer.element.impl.animation.animate-transform]
   [renderer.element.subs :as-alias element.subs]))

(element.hierarchy/derive-element
 ::element.hierarchy/animation
 ::element.hierarchy/descriptive)

(defmethod element.hierarchy/render ::element.hierarchy/animation
  [el]
  (let [{:keys [children tag attrs id]} el
        child-elements @(rf/subscribe [::element.subs/filter-visible children])]
    [tag attrs
     (for [el child-elements]
       ^{:key id}
       [element.hierarchy/render el])]))
