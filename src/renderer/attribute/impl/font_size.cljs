(ns renderer.attribute.impl.font-size
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/font-size"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.utils.font :as utils.font]
   [renderer.utils.length :as utils.length]))

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :font-size]
  []
  [::description
   "The font-size attribute refers to the size of the font from baseline to
    baseline when multiple lines of text are set solid in a multiline layout
    environment."])

(defmethod attribute.hierarchy/update-attr [::element.hierarchy/element
                                            :font-size]
  [el attribute f & more]
  (let [font-size (:font-size (utils.font/get-computed-styles! el))
        font-size (utils.length/unit->px font-size)]
    (assoc-in el [:attrs attribute] (str (apply f font-size more)))))

(defmethod attribute.hierarchy/form-element [::element.hierarchy/element
                                             :font-size]
  [_ k v attrs]
  [attribute.views/select-input k v
   (merge attrs
          {:default-value "medium"
           :items [{:id :xx-small
                    :label [::xx-small "XX-Small"]
                    :value "xx-small"}
                   {:id :x-small
                    :label [::x-small "X-Small"]
                    :value "x-small"}
                   {:id :small
                    :label [::small "Small"]
                    :value "small"}
                   {:id :medium
                    :label [::medium "Medium"]
                    :value "medium"}
                   {:id :large
                    :label [::large "Large"]
                    :value "large"}
                   {:id :x-large
                    :label [::x-large "X-Large"]
                    :value "x-large"}
                   {:id :xx-large
                    :label [::xx-large "XX-Large"]
                    :value "xx-large"}
                   {:id :xxx-large
                    :label [::xxx-large "XXX-Large"]
                    :value "xxx-large"}]})])
