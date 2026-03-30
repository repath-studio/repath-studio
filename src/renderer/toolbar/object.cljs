(ns renderer.toolbar.object
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [re-frame.core :as rf]
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

#_(def group-actions
    [:object/group
     :object/ungroup])

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
    (->> (element.views/context-menu)
         (map views/dropdown-menu-item)
         (into [:> DropdownMenu/Content
                {:side "left"
                 :align "end"
                 :class "menu-content rounded-sm"
                 :on-key-down #(.stopPropagation %)
                 :on-escape-key-down #(.stopPropagation %)}
                [views/dropdownmenu-arrow]]))]])

(defn object-buttons []
  (let [md? @(rf/subscribe [::window.subs/md?])
        object-actions [index-actions
                        (when md? horizontal-alignment-actions)
                        (when md? vertical-alignment-actions)
                        boolean-actions]]
    (->> (keep identity object-actions)
         (interpose [:divider])
         (flatten)
         (map toolbar.views/action-button))))

(defn root []
  (let [md? @(rf/subscribe [::window.subs/md?])]
    [views/toolbar
     {:class "flex-col px-2 md:px-1 gap-2 md:gap-1"}
     (into [:<>] (object-buttons))
     (when-not md?
       [:<>
        [:span.h-divider]
        [more-button]])]))
