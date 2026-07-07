(ns renderer.attribute.impl.style
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/style"
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.theme.subs :as-alias theme.subs]
   [renderer.views :as views]))

(defmethod attribute.hierarchy/form-element [::element.hierarchy/element :style]
  [_ k v {:keys [disabled]}]
  (let [codemirror-theme @(rf/subscribe [::theme.subs/codemirror])]
    [:div.w-full.bg-primary.px-2.py-1
     {:class (when disabled "*:[&.CodeMirror]:opacity-50")}
     [views/cm-editor
      (str v)
      {:on-blur #(rf/dispatch [::element.events/set-attr k %])
       :props {:id (name k)}
       :options {:mode "css"
                 :placeholder (when-not v "multiple")
                 :readOnly disabled
                 :screenReaderLabel (name k)
                 :theme codemirror-theme}}]]))
