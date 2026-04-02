(ns renderer.history.views
  (:require
   ["@radix-ui/react-select" :as Select]
   ["react" :as react]
   ["react-d3-tree" :refer [Tree]]
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.dialog.events :as-alias dialog.events]
   [renderer.history.events :as-alias history.events]
   [renderer.history.handlers :as history.handlers]
   [renderer.history.subs :as-alias history.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.views :as views]))

(defn select-option
  [{:keys [index explanation]}]
  [:> Select/Item
   {:value index
    :class "menu-item px-2!"}
   [:> Select/ItemText (apply i18n.views/t explanation)]])

(defn select
  [label options open]
  [:> Select/Root
   {:on-value-change #(rf/dispatch [::history.events/go-to %])
    :on-open-change #(reset! open %)
    :disabled (empty? options)}
   [:> Select/Trigger
    {:aria-label (i18n.views/t label)
     :as-child true}
    [:div.w-4.m-0.bg-transparent.flex.items-center.hover:pt-1
     {:class "min-h-[inherit]"}
     [:> Select/Value ""]
     [:> Select/Icon
      [views/icon "chevron-down"]]]]
   [:> Select/Portal
    [:> Select/Content
     {:side "bottom"
      :alignOffset -24
      :position "popper"
      :class "menu-content rounded-sm select-content"
      :on-key-down #(.stopPropagation %)
      :on-escape-key-down #(.stopPropagation %)}
     [:> Select/ScrollUpButton
      {:class "select-scroll-button"}
      [views/icon "chevron-up"]]
     [:> Select/Viewport
      {:class "select-viewport"}
      (into [:> Select/Group]
            (map select-option options))]
     [:> Select/ScrollDownButton
      {:class "select-scroll-button"}
      [views/icon "chevron-down"]]
     [views/select-arrow]]]])

(defn action-button
  [action]
  (let [{:keys [icon label options event show-options options-label]} action]
    (reagent/with-let [open (reagent/atom false)]
      [:button.button.rounded-sm.items-center.px-1.gap-1.flex.w-auto
       {:title (i18n.views/t label)
        :class [(if show-options "px-1" "px-2")
                (when @open "bg-overlay!")]
        :on-click #(rf/dispatch event)
        :disabled (empty? options)}
       [views/icon icon]
       (when show-options
         [select options-label options open])])))

(defn node
  "https://bkrem.github.io/react-d3-tree/docs/interfaces/CustomNodeElementProps.html"
  [^js/CustomNodeElementProps props]
  (let [datum (.-nodeDatum props)
        active? (.-active datum)
        id (.-id datum)
        color (if active? "var(--color-accent)" (.-color datum))
        title (apply i18n.views/t (.-name datum))]
    (reagent/as-element
     [:circle.transition-fill
      {:class "hover:stroke-accent"
       :on-click #(rf/dispatch [::history.events/go-to id])
       :on-pointer-enter #(when-not active?
                            (rf/dispatch [::history.events/preview id]))
       :on-pointer-leave #(rf/dispatch [::history.events/reset-state id])
       :cx "0"
       :cy "0"
       :stroke color
       :stroke-width 4
       :fill color
       :r 18}
      [:title title]])))

(defn on-update
  "https://bkrem.github.io/react-d3-tree/docs/interfaces/TreeProps.html#onupdate"
  [target]
  (let [translate (.-translate target)
        zoom (.-zoom target)]
    (when (and translate zoom)
      (let [x (.-x translate)
            y (.-y translate)]
        (rf/dispatch-sync [::history.events/tree-view-updated zoom [x y]])))))

(defn center
  [ref]
  (when-let [dom-el (.-current ref)]
    (matrix/div [(.-clientWidth dom-el)
                 (.-clientHeight dom-el)] 2)))

(defn tree
  [ref]
  (let [tree-data @(rf/subscribe [::history.subs/tree-data])
        zoom @(rf/subscribe [::history.subs/zoom])
        dom-el (.-current ref)
        [x y] @(rf/subscribe [::history.subs/translate])
        translate #js {:x (or x (when dom-el (/ (.-clientWidth dom-el) 2)))
                       :y (or y (when dom-el (/ (.-clientHeight dom-el) 2)))}]
    [:> Tree
     {:data tree-data
      :collapsible false
      :orientation "vertical"
      :rootNodeClassName "root-node"
      :depthFactor -100
      :zoom zoom
      :translate translate
      :onUpdate on-update
      :separation #js {:nonSiblings 1
                       :siblings 1}
      :renderCustomNodeElement node}]))

(defn clear-dialog
  []
  {:title (i18n.views/t [::action-cannot-undone
                         "This action cannot be undone."])
   :description (i18n.views/t [::clear-history-description
                               "Are you sure you wish to clear the document
                                history?"])
   :confirm-label (i18n.views/t [::clear-history "Clear history"])
   :confirm-action [::history.events/clear]})

(defn legend
  []
  (let [start-color (history.handlers/age-ratio->color 0)
        end-color (history.handlers/age-ratio->color 1)]
    [:div.flex.flex-col
     [:div.flex.justify-between.text-2xs.text-foreground-muted
      [:div.flex.gap-1
       [:div.h-5.w-px {:style {:background start-color}}]
       [:span (i18n.views/t [::oldest "Oldest"])]]

      [:div.flex.gap-1
       [:span (i18n.views/t [::newest "Newest"])]
       [:div.h-5.w-px {:style {:background end-color}}]]]
     [:div.w-full.h-2
      {:style {:background (str "linear-gradient(to right, "
                                start-color ", "
                                end-color ")")}}]]))

(defn root
  []
  (let [ref (react/createRef)]
    [:div.flex.flex-col.h-full.p-2.gap-2.w-full
     [:div.flex-1
      {:ref ref
       :on-pointer-move #(.stopPropagation %)}
      [tree ref]]

     [:div.flex.gap-2.flex-col
      [legend]
      [views/button-group
       [:button.button.flex-1
        {:on-click #(rf/dispatch [::history.events/tree-view-updated
                                  0.5 (center ref)])}
        (i18n.views/t [::center-view "Center view"])]
       [:button.button.flex-1
        {:on-click #(rf/dispatch [::dialog.events/show-confirmation
                                  (clear-dialog)])}
        (i18n.views/t [::clear-history "Clear history"])]]]]))
