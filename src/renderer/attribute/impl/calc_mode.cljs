(ns renderer.attribute.impl.calc-mode
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/calcMode"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.hierarchy :as-alias element.hierarchy]))

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :calcMode]
  []
  [::description "The calcMode attribute specifies the interpolation mode for
                  the animation."])

(defmethod attribute.hierarchy/form-element [::element.hierarchy/element
                                             :calcMode]
  [_ k v {:keys [disabled]}]
  [attribute.views/select-input k v
   {:disabled disabled
    :placeholder "linear"
    :default-value "linear"
    :items [{:key :discrete
             :value "discrete"
             :label [::discrete "discrete"]}
            {:key :linear
             :value "linear"
             :label [::linear "linear"]}
            {:key :paced
             :value "paced"
             :label [::paced "paced"]}
            {:key :spline
             :value "spline"
             :label [::spline "spline"]}]}])
