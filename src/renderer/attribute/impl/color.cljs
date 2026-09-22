(ns renderer.attribute.impl.color
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#color"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as app.subs]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.events :as events]
   [renderer.hierarchy :as hierarchy]
   [renderer.i18n.views :as i18n.views]
   [renderer.utils.color :as utils.color]
   [renderer.views :as views]))

(hierarchy/derive! :stroke ::color)
(hierarchy/derive! :fill ::color)
(hierarchy/derive! :color ::color)

(defn color-url
  [notation]
  (if (= notation "hex")
    "https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Values/hex-color"
    (str "https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Values/color_value/"
         notation)))

(defn mdn-color-button
  [notation]
  (let [url (color-url notation)]
    [:button.button.px-3.flex-1.rounded
     {:on-click #(rf/dispatch [::events/open-remote-url url])}
     (i18n.views/t [::learn-more "Learn more about %1"]
                   [(string/upper-case notation)])]))

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
        [views/color-picker
         {:value color
          :dropper dropper?
          :on-change #(rf/dispatch [::element.events/preview-attr k %])
          :on-commit #(rf/dispatch [::element.events/set-attr k %])}
         [mdn-color-button (utils.color/string->notation v)]]
        [views/popover-arrow]]]]]))
