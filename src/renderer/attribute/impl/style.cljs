(ns renderer.attribute.impl.style
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/style"
  (:require
   ["@codemirror/lang-css" :refer [css]]
   ["@codemirror/state" :refer [EditorState]]
   ["@codemirror/view" :refer [EditorView placeholder]]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.theme.subs :as-alias theme.subs]
   [renderer.views :as views]))

(defmethod attribute.hierarchy/form-element [::element.hierarchy/element :style]
  [_ k v {:keys [disabled]}]
  (let [theme-mode @(rf/subscribe [::theme.subs/computed-mode])]
    [:div.w-full.bg-primary.px-1
     {:class ["py-px"
              (when disabled "*:opacity-50")]}
     [views/cm-editor
      (str v)
      {:on-blur #(->> (.. ^js %2 -state -doc toString)
                      (conj [::element.events/set-attr k])
                      (rf/dispatch))
       :on-keydown #(.stopPropagation %)
       :on-keyup #(.stopPropagation %)
       :props {:id (name k)}
       :theme-mode theme-mode
       :extensions [(css)
                    (placeholder (when-not v "multiple"))
                    (EditorState.readOnly.of disabled)
                    (EditorView.editable.of (not disabled))
                    (EditorView.contentAttributes.of #js {:aria-label
                                                          "Style"})]}]]))
