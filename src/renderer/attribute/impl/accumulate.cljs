(ns renderer.attribute.impl.accumulate
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/accumulate"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.hierarchy :as-alias element.hierarchy]))

(defmethod attribute.hierarchy/description [::element.hierarchy/animation
                                            :accumulate]
  []
  [::description "The accumulate attribute controls whether or not an animation
                  is cumulative."])

(defmethod attribute.hierarchy/form-element [::element.hierarchy/animation
                                             :accumulate]
  [_ k v {:keys [disabled]}]
  [attribute.views/select-input k v
   {:disabled disabled
    :placeholder "none"
    :default-value "none"
    :items [{:key :none
             :value "none"
             :label [::none "None"]}
            {:key :sum
             :value "sum"
             :label [::sum "Sum"]}]}])
