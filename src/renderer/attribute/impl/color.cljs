(ns renderer.attribute.impl.color
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#color"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   [re-frame.core :as rf]
   [renderer.app.subs :as app.subs]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.color-picker-view :as color-picker-view]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.hierarchy :as hierarchy]
   [renderer.i18n.views :as i18n.views]
   [renderer.views :as views]))

(hierarchy/derive! :stroke ::color)
(hierarchy/derive! :fill ::color)
(hierarchy/derive! :color ::color)

(defmethod attribute.hierarchy/form-element [::element.hierarchy/element
                                             ::color]
  [_ k v attrs]
  (let [color (if (empty? v) (:placeholder attrs) v)
        dropper? @(rf/subscribe [::app.subs/supported-feature? :eye-dropper])]
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
          :style {:background color}}]]]
      [:> Popover/Portal
       [:> Popover/Content
        {:sideOffset 5
         :class "popover-content"
         :align "end"
         :on-escape-key-down #(.stopPropagation %)}
        [color-picker-view/root
         {:value color
          :dropper dropper?
          :on-change #(rf/dispatch [::element.events/preview-attr k %])
          :on-commit #(rf/dispatch [::element.events/set-attr k %])}]
        [views/popover-arrow]]]]]))
