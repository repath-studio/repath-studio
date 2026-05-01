(ns renderer.attribute.impl.font-style
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/font-style"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.utils.font :as utils.font]
   [renderer.utils.length :as utils.length]))

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :font-style]
  []
  [::description
   "The font-size attribute refers to the size of the font from baseline to
    baseline when multiple lines of text are set solid in a multiline layout
    environment."])

(defmethod attribute.hierarchy/update-attr [::element.hierarchy/element
                                            :font-style]
  [el attribute f & more]
  (let [font-size (:font-size (utils.font/get-computed-styles! el))
        font-size (utils.length/unit->px font-size)]
    (assoc-in el [:attrs attribute] (str (apply f font-size more)))))

(defmethod attribute.hierarchy/form-element [::element.hierarchy/element
                                             :font-style]
  [_ k v attrs]
  [attribute.views/select-input k v
   (merge attrs
          {:default-value "normal"
           :items [{:id :normal
                    :label [::normal "Normal"]
                    :value "normal"}
                   {:id :italic
                    :label [::italic "Italic"]
                    :value "italic"}
                   {:id :oblique
                    :label [::oblique "Oblique"]
                    :value "oblique"}]})])
