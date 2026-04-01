(ns renderer.toolbar.object
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [re-frame.core :as rf]
   [renderer.action.views :as action.views]
   [renderer.element.views :as element.views]
   [renderer.i18n.views :as i18n.views]
   [renderer.toolbar.views :as toolbar.views]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(def index-actions
  [:object/raise-to-top
   :object/lower-to-bottom
   :object/raise
   :object/lower])

(def horizontal-alignment-actions
  [:align/left
   :align/center-horizontal
   :align/right])

(def vertical-alignment-actions
  [:align/top
   :align/center-vertical
   :align/bottom])

(def boolean-actions
  [:boolean/unite
   :boolean/intersect
   :boolean/subtract
   :boolean/exclude
   :boolean/divide])

(defn more-button []
  [:> DropdownMenu/Root
   [:> DropdownMenu/Trigger
    {:as-child true}
    [:button.button.flex.items-center.justify-center.px-2.font-mono.rounded
     {:aria-label (i18n.views/t [::more-actions "More object actions"])}
     [views/icon "ellipsis-h"]]]
   [:> DropdownMenu/Portal
    (->> element.views/context-menu-actions
         (map action.views/entity)
         (map views/dropdown-menu-item)
         (into [:> DropdownMenu/Content
                {:side "left"
                 :align "end"
                 :class "menu-content rounded-sm"
                 :on-key-down #(.stopPropagation %)
                 :on-escape-key-down #(.stopPropagation %)}
                [views/dropdownmenu-arrow]]))]])

(defn button-group
  [ids]
  (->> (map toolbar.views/button ids)
       (into [:<>])))

(defn root []
  (let [md? @(rf/subscribe [::window.subs/md?])
        groups [(when md? index-actions)
                horizontal-alignment-actions
                vertical-alignment-actions
                boolean-actions]]
    [views/toolbar
     {:class "flex-col px-2 md:px-1 gap-2 md:gap-1"}
     (->> groups
          (keep identity)
          (map button-group)
          (interpose [:span.h-divider])
          (into [:<>]))
     (when-not md?
       [:<>
        [:span.h-divider]
        [more-button]])]))
