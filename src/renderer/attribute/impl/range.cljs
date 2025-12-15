(ns renderer.attribute.impl.range
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]))

(attribute.hierarchy/derive-attribute :opacity ::range)

(defmethod attribute.hierarchy/form-element [:default ::range]
  [_ k v attrs]
  [attribute.views/range-input k v
   (merge attrs {:min 0
                 :max 1
                 :step 0.01})])
