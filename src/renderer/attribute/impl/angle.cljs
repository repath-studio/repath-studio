(ns renderer.attribute.impl.angle
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#angle"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.i18n.views :as i18n.views]
   [renderer.views :as views]))

(defmethod attribute.hierarchy/form-element [::element.hierarchy/element
                                             ::angle]
  [_ k v attrs]
  [:div.flex.gap-px.w-full
   [attribute.views/form-input k v attrs]
   [:> Popover/Root {:modal true}
    [:> Popover/Trigger
     {:title (i18n.views/t [::pick-angle "Pick angle"])
      :class "form-control-button"}
     [views/icon "degrees"]]
    [:> Popover/Portal
     [:> Popover/Content
      {:sideOffset 5
       :class "popover-content"
       :align "end"
       :on-escape-key-down #(.stopPropagation %)}
      [:div.circular-slider]
      [views/popover-arrow]]]]])
