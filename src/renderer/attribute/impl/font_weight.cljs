(ns renderer.attribute.impl.font-weight
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/font-weight"
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.utils.attribute :as utils.attribute]))

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :font-weight]
  []
  [::description
   "The font-weight attribute refers to the boldness or lightness of the
    glyphs used to render the text, relative to other fonts in the same font
    family."])

(defn label
  [weight]
  (let [weight-name (first (get utils.attribute/weight-name-mapping weight))]
    (str weight " - " weight-name)))

(defmethod attribute.hierarchy/form-element [::element.hierarchy/element
                                             :font-weight]
  [_ k v attrs]
  (let [available-weights @(rf/subscribe [::element.subs/font-weights])
        weights (sort (if (seq available-weights)
                        available-weights
                        (keys utils.attribute/weight-name-mapping)))]
    [attribute.views/select-input k v
     (merge attrs
            {:default-value "400"
             :items (mapv #(do {:key %
                                :label [k (label %)]
                                :value %}) weights)})]))
