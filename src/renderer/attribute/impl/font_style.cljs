(ns renderer.attribute.impl.font-style
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/font-style"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.hierarchy :as-alias element.hierarchy]))

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :font-style]
  []
  [::description
   "The font-style attribute specifies whether the text is to be rendered using
    a normal, italic, or oblique face."])

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
