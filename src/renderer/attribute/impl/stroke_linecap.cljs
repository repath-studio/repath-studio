(ns renderer.attribute.impl.stroke-linecap
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/stroke-linecap"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.hierarchy :as-alias element.hierarchy]))

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :stroke-linecap]
  []
  [::description
   "The stroke-linecap attribute is a presentation attribute defining the
    shape to be used at the end of open subpaths when they are stroked."])

(defmethod attribute.hierarchy/form-element [::element.hierarchy/element
                                             :stroke-linecap]
  [_ k v attrs]
  [attribute.views/select-input k v
   (merge attrs {:default-value "butt"
                 :items [{:key :butt
                          :value "butt"
                          :label [::butt "Butt"]
                          :icon "linecap-butt"}
                         {:key :round
                          :value "round"
                          :label [::round "Round"]
                          :icon "linecap-round"}
                         {:key :square
                          :value "square"
                          :label [::square "Square"]
                          :icon "linecap-square"}]})])
