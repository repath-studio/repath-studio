(ns renderer.attribute.impl.transform
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/transform"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.utils.transform :as utils.transform]))

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :transform]
  []
  [::description
   "The transform attribute defines a list of transform definitions that are
    applied to an element and the element's children."])

(defmethod attribute.hierarchy/update-attr [::element.hierarchy/element
                                            :transform]
  [el k transform]
  (let [el (update-in el [:attrs k] utils.transform/compose-attr transform)]
    (cond-> el
      (nil? (-> el :attrs k))
      (update :attrs dissoc k))))
