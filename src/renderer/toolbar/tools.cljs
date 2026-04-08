(ns renderer.toolbar.tools
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-tooltip" :as Tooltip]
   ["react" :as react]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.action.views :as action.views]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.dom :as utils.dom]
   [renderer.views :as views]))

(defn button
  [action bordered]
  (let [active (action.views/checked? action)
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
        [action.views/label action]
        [views/shortcuts action]]]]]))

(defn button-group
  [action-group]
  (some->> (:actions action-group)
           (map (fn [action] [button action false]))
           (into [:div {:class "flex justify-center md:gap-1 gap-0.5"}])))

(defn tool-action
  [actions tool-id]
  (some #(when (= tool-id (keyword (name (:id %)))) %) actions))

(defn dropdown-button
  [{:keys [label actions]}]
  (let [active-tool @(rf/subscribe [::tool.subs/active])
        cached-tool @(rf/subscribe [::tool.subs/cached])
        active-action (tool-action actions active-tool)
        cached-action (tool-action actions cached-tool)
        top-tool (or active-action cached-action (first actions))]
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
                     (when cached-action "outline outline-inset outline-accent")
                     (when active-action
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
      [button top-tool true])))

(def action-groups
  [:tools/transform
   :tools/containers
   :tools/elements
   :tools/draw
   :tools/misc])

(defn root
  []
  (let [overflow? (reagent/atom false)
        measure-ref (react/createRef)
        observer (js/ResizeObserver.
                  (fn [_entries]
                    (let [el (.-current measure-ref)]
                      (reset! overflow?
                              (utils.dom/content-overflow? el)))))]
    (reagent/create-class
     {:component-did-mount
      #(.observe observer (.-current measure-ref))

      :component-will-unmount
      #(.disconnect observer)

      :reagent-render
      (fn []
        (let [groups (keep action.views/deref-action-group action-groups)]
          [:div.relative
           (->> groups
                (map button-group)
                (interpose [:span.v-divider])
                (into [views/toolbar
                       {:ref measure-ref
                        :class "absolute invisible w-full overflow-hidden"}]))
           (if @overflow?
             (->> groups
                  (map dropdown-button)
                  (into [views/toolbar
                         {:class "bg-primary justify-center py-2 gap-2"}]))
             (->> groups
                  (map button-group)
                  (interpose [:span.v-divider])
                  (into [views/toolbar
                         {:class "justify-center bg-primary"}])))]))})))
