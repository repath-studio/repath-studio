(ns renderer.element.impl.descriptive.core
  (:require
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.impl.descriptive.mpath]
   [renderer.hierarchy :as hierarchy]))

(hierarchy/derive! ::element.hierarchy/descriptive
                   ::element.hierarchy/renderable)
(hierarchy/derive! :desc ::element.hierarchy/descriptive)
(hierarchy/derive! :metadata ::element.hierarchy/descriptive)
(hierarchy/derive! :title ::element.hierarchy/descriptive)

(defmethod element.hierarchy/permitted-content :element.hierarchy/descriptive
  [_el]
  #{::element.hierarchy/descriptive})
