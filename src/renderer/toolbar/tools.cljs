(ns renderer.toolbar.tools
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-tooltip" :as Tooltip]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(defn button
  [bordered tool]
  (let [active-tool @(rf/subscribe [::tool.subs/active])
        cached-tool @(rf/subscribe [::tool.subs/cached])
        active (= active-tool tool)
        primary (= cached-tool tool)
        properties (tool.hierarchy/properties tool)
        label (or (some-> properties :label i18n.views/t)
                  (string/capitalize (name tool)))]
    (when (:icon properties)
      [:> Tooltip/Root
       [:> Tooltip/Trigger
        {:as-child true}
        [:span
         [views/radio-icon-button (:icon properties) active
          {:class [(when primary "outline outline-inset outline-accent")
                   (when bordered "border border-border")]
           :aria-label label
           :on-click #(rf/dispatch [::tool.events/activate tool])}]]]
       [:> Tooltip/Portal
        [:> Tooltip/Content
         {:class "tooltip-content"
          :sideOffset 10
          :side "top"
          :on-escape-key-down #(.stopPropagation %)}
         [:div.flex.gap-2.items-center
          label
          [views/shortcuts [::tool.events/activate tool]]]]]])))

(defn button-group
  [items]
  (into [:div {:class "flex justify-center md:gap-1 gap-0.5"}]
        (map (partial button false) items)))

(defn dropdown-button
  [{:keys [label tools]}]
  (let [active-tool @(rf/subscribe [::tool.subs/active])
        contains-active? (some #{active-tool} tools)
        top-tool (if contains-active? active-tool (first tools))
        icon (:icon (tool.hierarchy/properties top-tool))]
    (if (second tools)
      [:> DropdownMenu/Root
       [:> DropdownMenu/Trigger
        {:as-child true}
        [:button.button.flex.items-center.justify-center.px-2.font-mono
         {:class ["rounded-sm gap-1 border border-border"
                  (when contains-active?
                    "bg-accent text-accent-foreground! hover:bg-accent-light
                     aria-expanded:bg-accent-light active:bg-accent-light")]
          :aria-label (i18n.views/t label)}
         [views/icon icon]
         [views/icon "chevron-down"]]]
       [:> DropdownMenu/Portal
        (->> tools
             (mapv (fn [tool]
                     (let [properties (tool.hierarchy/properties tool)]
                       {:label (:label properties)
                        :action [::tool.events/activate tool]
                        :icon (:icon properties)})))
             (into [:> DropdownMenu/Content
                    {:side "bottom"
                     :align "middle"
                     :class "menu-content rounded-sm"
                     :on-key-down #(.stopPropagation %)
                     :on-escape-key-down #(.stopPropagation %)}
                    [views/dropdownmenu-arrow]]
                   (map views/dropdown-menu-item)))]]
      [button true top-tool])))

(defn groups []
  [{:id :transform
    :label [::transform "Transform tools"]
    :tools [:transform :edit :pan :zoom]}
   {:id :containers
    :label [::containers "Container tools"]
    :tools [:svg]}
   {:id :shapes
    :label [::shapes "Shape tools"]
    :tools [:circle :ellipse :rect :line :polyline :polygon :image :text]}
   {:id :drawing
    :label [::drawing "Drawing tools"]
    :tools [:brush :pen]}
   {:id :misc
    :label [::misc "Miscallaneous tools"]
    :tools (cond-> [:fill :measure]
             @(rf/subscribe [::app.subs/supported-feature? :eye-dropper])
             (conj :dropper))}])

(defn root
  []
  (let [xl @(rf/subscribe [::window.subs/xl?])]
    (if xl
      (->> (groups)
           (map :tools)
           (map button-group)
           (interpose [:span.v-divider])
           (into [views/toolbar {:class "justify-center bg-primary"}]))
      (->> (groups)
           (map dropdown-button)
           (into [views/toolbar {:class "bg-primary justify-center py-2
                                         gap-2"}])))))
