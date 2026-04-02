(ns renderer.snap.views
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.action.views :as action.views]
   [renderer.document.subs :as-alias document.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.snap.subs :as-alias snap.subs]
   [renderer.utils.svg :as utils.svg]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(def snap-options
  [:snap/toggle-centers
   :snap/toggle-midpoints
   :snap/toggle-corners
   :snap/toggle-nodes])

(defn root []
  (let [active? (rf/subscribe [::snap.subs/active?])
        md? @(rf/subscribe [::window.subs/md?])
        toggle-snap-action (action.views/entity :snap/toggle)]
    (reagent/with-let [open (reagent/atom false)]
      [:button.button.rounded-sm.items-center.gap-1.md:flex.justify-items-center
       {:title (action.views/label toggle-snap-action)
        :class ["active:bg-overlay"
                (when md? "px-1")
                (when @active? "accent")
                (when @open "bg-overlay!")]
        :on-click (action.views/dispatch toggle-snap-action)}
       [views/icon (:icon toggle-snap-action)]
       (when md?
         [:> DropdownMenu/Root
          {:on-open-change #(reset! open %)}
          [:> DropdownMenu/Trigger
           {:as-child true}
           [:div.h-full.flex.items-center.hover:pb-1
            {:class "min-h-[inherit]"
             :role "button"
             :title (i18n.views/t [::snap-options "Snap options"])}
            [views/icon "chevron-up"]]]
          [:> DropdownMenu/Portal
           (->> snap-options
                (map (comp views/dropdown-menu-item action.views/entity))
                (into [:> DropdownMenu/Content
                       {:side "top"
                        :align "end"
                        :sideOffset 5
                        :alignOffset -5
                        :position "popper"
                        :class "menu-content rounded-sm select-content"
                        :on-key-down #(.stopPropagation %)
                        :on-escape-key-down #(.stopPropagation %)}
                       [views/dropdownmenu-arrow]]))]])])))

(defn canvas-label
  [nearest-neighbor]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        margin (/ 15 zoom)
        point-label (-> nearest-neighbor meta :label)
        base-label (-> nearest-neighbor :base-point meta :label)
        point (:point nearest-neighbor)
        [x y] (matrix/add point margin)
        label (->> [base-label point-label]
                   (remove nil?)
                   (map i18n.views/t)
                   (string/join (i18n.views/t [::to " to "])))]
    [:<>
     [utils.svg/times point]
     (when (not-empty label)
       [utils.svg/label label {:x x
                               :y y
                               :text-anchor "start"
                               :font-family "var(--font-sans)"}])]))
