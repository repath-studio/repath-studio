(ns renderer.toolbar.tools
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-tooltip" :as Tooltip]
   [re-frame.core :as rf]
   [renderer.action.views :as action.views]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(defn button
  [bordered action]
  (let [xl? @(rf/subscribe [::window.subs/xl?])
        active (action.views/checked? action)
        cached-tool @(rf/subscribe [::tool.subs/cached])
        primary (= cached-tool (keyword (name (:id action))))]
    [:> Tooltip/Root
     [:> Tooltip/Trigger
      {:as-child true}
      [:span
       [views/radio-icon-button (:icon action) active
        {:class [(when primary "outline outline-inset outline-accent")
                 (when bordered "border border-border")]
         :aria-label (action.views/label action)
         :on-click (action.views/dispatch action)}]]]
     [:> Tooltip/Portal
      [:> Tooltip/Content
       {:class "tooltip-content"
        :sideOffset 10
        :side "top"
        :on-escape-key-down #(.stopPropagation %)}
       [:div.flex.gap-2.items-center
        (action.views/label action)
        (when xl? [views/shortcuts action])]]]]))

(defn button-group
  [action-group]
  (some->> (:actions action-group)
           (map (partial button false))
           (into [:div {:class "flex justify-center md:gap-1 gap-0.5"}])))

(defn dropdown-button
  [{:keys [label actions]}]
  (let [active-tool @(rf/subscribe [::tool.subs/active])
        contains-active? (some #{active-tool} actions)
        top-tool (if contains-active? active-tool (first actions))]
    (if (second actions)
      [:> DropdownMenu/Root
       [:> DropdownMenu/Trigger
        {:as-child true}
        [:div
         [:> Tooltip/Root
          [:> Tooltip/Trigger
           {:as-child true}
           [:button.button.flex.items-center.justify-center.px-2.font-mono
            {:class ["rounded-sm gap-1 border border-border"
                     (when contains-active?
                       "bg-accent text-accent-foreground! hover:bg-accent-light
                       aria-expanded:bg-accent-light active:bg-accent-light")]}
            [views/icon (:icon top-tool)]
            [views/icon "chevron-down"]]]
          [:> Tooltip/Portal
           [:> Tooltip/Content
            {:class "tooltip-content"
             :sideOffset 10
             :side "top"
             :on-escape-key-down #(.stopPropagation %)}
            [:div.flex.gap-2.items-center
             (i18n.views/t label)]]]]]]

       [:> DropdownMenu/Portal
        (->> actions
             (map views/dropdown-menu-item)
             (into [:> DropdownMenu/Content
                    {:side "bottom"
                     :align "middle"
                     :class "menu-content rounded-sm"
                     :on-key-down #(.stopPropagation %)
                     :on-escape-key-down #(.stopPropagation %)}
                    [views/dropdownmenu-arrow]]))]]
      [button true top-tool])))

(def action-groups
  [:tools/transform
   :tools/containers
   :tools/elements
   :tools/draw
   :tools/misc])

(defn root
  []
  (let [xl @(rf/subscribe [::window.subs/xl?])]
    (if xl
      (->> action-groups
           (keep (comp button-group action.views/deref-action-group))
           (interpose [:span.v-divider])
           (into [views/toolbar {:class "justify-center bg-primary"}]))
      (->> action-groups
           (keep (comp dropdown-button action.views/deref-action-group))
           (into [views/toolbar {:class "bg-primary justify-center py-2
                                         gap-2"}])))))
