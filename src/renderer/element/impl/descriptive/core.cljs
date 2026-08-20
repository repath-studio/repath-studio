(ns renderer.element.impl.descriptive.core
  (:require
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.impl.descriptive.mpath]))

(defmethod element.hierarchy/permitted-content :element.hierarchy/descriptive
  [_el]
  #{::element.hierarchy/descriptive})
