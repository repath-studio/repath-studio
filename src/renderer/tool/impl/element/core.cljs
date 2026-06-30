(ns renderer.tool.impl.element.core
  (:require
   ["@radix-ui/react-select" :as Select]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.handlers :as element.handlers]
   [renderer.hierarchy :as hierarchy]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.element.circle]
   [renderer.tool.impl.element.ellipse]
   [renderer.tool.impl.element.image]
   [renderer.tool.impl.element.line]
   [renderer.tool.impl.element.path]
   [renderer.tool.impl.element.poly]
   [renderer.tool.impl.element.polygon]
   [renderer.tool.impl.element.polyline]
   [renderer.tool.impl.element.rect]
   [renderer.tool.impl.element.svg]
   [renderer.tool.impl.element.text]
   [renderer.views :as views]))

(hierarchy/derive! ::tool.hierarchy/element ::tool.hierarchy/tool)

(defn line
  [stroke-width]
  [:div.bg-foreground-muted.flex-1
   {:style {:height stroke-width}}])

(defn select-item
  [value]
  [:> Select/Item
   {:value value
    :class "menu-item px-2!"}
   [:div.flex.items-center.gap-2.w-26
    [:> Select/ItemText value]
    [line value]]])

(defmethod tool.hierarchy/tool-options ::tool.hierarchy/element
  []
  (let [stroke-width @(rf/subscribe [::document.subs/attr :stroke-width])]
    [:> Select/Root
     {:value stroke-width
      :onValueChange #(rf/dispatch [::document.events/set-attr
                                    :stroke-width %])}
     [:> Select/Trigger
      {:class "form-control-button rounded-sm px-2!"
       :aria-label (i18n.views/t [::select-stroke-width "Select stroke width"])}
      [:div.flex.items-center.gap-2.w-26
       [:> Select/Value stroke-width]
       [line stroke-width]
       [:> Select/Icon
        [views/icon "chevron-down"]]]]
     [:> Select/Portal
      [:> Select/Content
       {:class "menu-content rounded-sm select-content"
        :on-key-down #(.stopPropagation %)
        :on-escape-key-down #(.stopPropagation %)}
       (->> (map select-item ["1px" "2px" "4px" "8px"])
            (into [:> Select/Viewport {:class "select-viewport"}]))]]]))

(defmethod tool.hierarchy/help [::tool.hierarchy/element :idle]
  []
  (i18n.views/t [::click-and-drag "Click and drag to create an element."]))

(defmethod tool.hierarchy/help [::tool.hierarchy/element :create]
  []
  (i18n.views/t [::release-to-finalize "Release to finalize the element."]))

(defmethod tool.hierarchy/on-activate ::tool.hierarchy/element
  [db]
  (tool.handlers/set-cursor db "crosshair"))

(defn snap-point
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label [::edge "edge"]})])

(defmethod tool.hierarchy/snapping-points [::tool.hierarchy/element :idle]
  [db]
  (snap-point db))

(defmethod tool.hierarchy/snapping-points [::tool.hierarchy/element :create]
  [db]
  (snap-point db))

(defmethod tool.hierarchy/snapping-elements [::tool.hierarchy/element :idle]
  [db]
  (element.handlers/visible db))

(defmethod tool.hierarchy/snapping-elements [::tool.hierarchy/element :create]
  [db]
  (element.handlers/visible db))

(rf/dispatch [::action.events/register-action-group
              {:id :tools/elements
               :icon "shapes"
               :label [::elements "Elements"]
               :actions [:tool/circle
                         :tool/ellipse
                         :tool/rect
                         :tool/line
                         :tool/path
                         :tool/polyline
                         :tool/polygon
                         :tool/image
                         :tool/text]}])

(rf/dispatch [::action.events/register-action-group
              {:id :tools/containers
               :label [::containers "Containers"]
               :actions [:tool/svg]}])
