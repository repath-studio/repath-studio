(ns renderer.attribute.impl.crossorigin
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/crossorigin"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.hierarchy :as-alias element.hierarchy]))

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :crossorigin]
  []
  [::description
   "The crossorigin attribute, valid on the <image> and <feImage> elements,
    provides support for configuration of the Cross-Origin Resource Sharing
    (CORS) requests for the element's fetched data."])

(defmethod attribute.hierarchy/form-element [::element.hierarchy/element
                                             :crossorigin]
  [_ k v attrs]
  [attribute.views/select-input k v
   (merge attrs {:items [{:key :anonymous
                          :value "anonymous"
                          :label [::anonymous "Anonymous"]}
                         {:key :use-credentials
                          :value "use-credentials"
                          :label [::use-credentials "Use credentials"]}]})])
