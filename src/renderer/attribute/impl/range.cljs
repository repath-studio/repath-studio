(ns renderer.attribute.impl.range
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.hierarchy :as hierarchy]))

(hierarchy/derive! :opacity ::range)

(defmethod attribute.hierarchy/form-element [::element.hierarchy/element
                                             ::range]
  [_ k v attrs]
  [attribute.views/range-input k v
   (merge attrs {:min 0
                 :max 1
                 :step 0.01})])
