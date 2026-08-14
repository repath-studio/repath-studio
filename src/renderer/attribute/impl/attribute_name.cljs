(ns renderer.attribute.impl.attribute-name
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/attributeName"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   ["cmdk" :as Command]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.i18n.views :as i18n.views]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.views :as views]))

(defmethod attribute.hierarchy/description [::element.hierarchy/animation
                                            :attributeName]
  []
  [::description "The attributeName attribute indicates the name of the CSS
                  property or attribute of the target element that is going to
                  be changed during an animation."])

(defn attr-item
  [s]
  [:> Command/CommandItem
   {:class "flex p-2 rounded-sm items-center justify-between
            data-[selected=true]:bg-overlay"
    :on-select #(rf/dispatch [::element.events/set-attr :attributeName s])}
   [:div.flex.justify-between.items-center.w-full.gap-2 s]])

(defn suggestions-list
  []
  [:div.flex.flex-col
   [:> Command/Command
    {:label "Command Menu"
     :on-key-down #(.stopPropagation %)}
    [:> Command/CommandInput
     {:class "p-3 bg-primary border-b border-border w-full"
      :placeholder (i18n.views/t [::search-attribute "Search for an attribute"])}]
    [:div.flex.max-h-80.overflow-hidden
     [views/scroll-area
      [:> Command/CommandList
       {:class "p-1"}
       [:> Command/CommandEmpty
        (i18n.views/t [::no-attributes "No attributes found."])]
       (for [attr utils.attribute/presentation]
         ^{:key attr}
         [attr-item (name attr)])]]]]])

(defmethod attribute.hierarchy/form-element [::element.hierarchy/animation
                                             :attributeName]
  [_ k v attrs]
  [:div.flex.gap-px.w-full
   [attribute.views/form-input k v attrs]
   [:> Popover/Root
    {:modal true}
    [:> Popover/Trigger
     {:title (i18n.views/t [::select-attribute "Select attribute"])
      :class "form-control-button"
      :disabled (:disabled attrs)}
     [views/icon "magnifier"]]
    [:> Popover/Portal
     [:> Popover/Content
      {:sideOffset 5
       :class "popover-content"
       :align "end"
       :on-escape-key-down #(.stopPropagation %)}
      [suggestions-list]
      [views/popover-arrow]]]]])
