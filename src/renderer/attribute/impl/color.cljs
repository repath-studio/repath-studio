(ns renderer.attribute.impl.color
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#color"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   ["@repath-studio/react-color" :refer [ChromePicker]]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.i18n.views :as i18n.views]
   [renderer.views :as views]))

(attribute.hierarchy/derive-attribute :stroke ::color)
(attribute.hierarchy/derive-attribute :fill ::color)
(attribute.hierarchy/derive-attribute :color ::color)

(defn picker
  [k v]
  [:> ChromePicker
   {:color (or v "")
    :on-change-complete #(rf/dispatch [::element.events/set-attr k (.-hex %)])
    :on-change #(rf/dispatch [::element.events/preview-attr k (.-hex %)])}])

(defmethod attribute.hierarchy/form-element [:default ::color]
  [_ k v attrs]
  [:div.flex.gap-px.w-full
   [attribute.views/form-input k v attrs]
   [:> Popover/Root {:modal true}
    [:> Popover/Trigger
     {:as-child true}
     [:button.form-control-button
      {:class "p-1.5"
       :disabled (:disabled attrs)
       :title (i18n.views/t [::pick-color "Pick color"])}
      [:div.w-full.h-full.bg-overlay.rounded-xs
       {:class (when (:disabled attrs) "opacity-30")
        :style {:background v}}]]]
    [:> Popover/Portal
     [:> Popover/Content
      {:sideOffset 5
       :class "popover-content"
       :align "end"
       :on-escape-key-down #(.stopPropagation %)}
      [:div.p-2
       {:dir "ltr"
        :tab-index 0}
       [picker k v]]
      [views/popover-arrow]]]]])
