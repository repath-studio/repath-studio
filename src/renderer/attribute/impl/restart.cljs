(ns renderer.attribute.impl.restart
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/restart"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.hierarchy :as-alias element.hierarchy]))

(defmethod attribute.hierarchy/description [::element.hierarchy/animation
                                            :restart]
  []
  [::description "The restart attribute specifies whether or not an animation
                  can restart."])

(defmethod attribute.hierarchy/form-element [::element.hierarchy/animation
                                             :restart]
  [_ k v {:keys [disabled]}]
  [attribute.views/select-input k v
   {:disabled disabled
    :placeholder "always"
    :default-value "always"
    :items [{:key :always
             :value "always"
             :label [::always "Always"]}
            {:key :whenNotActive
             :value "whenNotActive"
             :label [::when-not-active "When not active"]}
            {:key :never
             :value "never"
             :label [::never "Never"]}]}])
