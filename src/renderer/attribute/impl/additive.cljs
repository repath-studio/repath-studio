(ns renderer.attribute.impl.additive
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/additive"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.hierarchy :as-alias element.hierarchy]))

(defmethod attribute.hierarchy/description [::element.hierarchy/animation
                                            :additive]
  []
  [::description "The additive attribute controls whether or not an animation is
                  additive."])

(defmethod attribute.hierarchy/form-element [::element.hierarchy/animation
                                             :additive]
  [_ k v {:keys [disabled]}]
  [attribute.views/select-input k v
   {:disabled disabled
    :placeholder "replace"
    :default-value "replace"
    :items [{:key :replace
             :value "replace"
             :label [::replace "Replace"]}
            {:key :sum
             :value "sum"
             :label [::sum "Sum"]}]}])
