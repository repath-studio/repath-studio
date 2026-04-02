(ns renderer.toolbar.status
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-popover" :as Popover]
   ["@radix-ui/react-tooltip" :as Tooltip]
   ["@repath-studio/react-color" :refer [ChromePicker PhotoshopPicker]]
   [re-frame.core :as rf]
   [renderer.action.views :as action.views]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.events :as-alias element.events]
   [renderer.frame.events :as-alias frame.events]
   [renderer.i18n.views :as i18n.views]
   [renderer.snap.views :as snap.views]
   [renderer.timeline.views :as timeline.views]
   [renderer.utils.key :as utils.key]
   [renderer.utils.length :as utils.length]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(defn coordinates []
  (let [[x y] @(rf/subscribe [::app.subs/adjusted-pointer-pos])]
    [:div.flex-col.font-mono.leading-tight.hidden
     {:class "xl:flex"
      :style {:min-width "90px"}
      :dir "ltr"}
     [:div.flex.justify-between
      [:span.mr-1 "X:"] [:span (utils.length/->fixed x 2 false)]]
     [:div.flex.justify-between
      [:span.mr-1 "Y:"] [:span (utils.length/->fixed y 2 false)]]]))

(defn zoom-menu []
  [:> DropdownMenu/Root
   [:> DropdownMenu/Trigger
    {:title (i18n.views/t [::select-zoom "Select zoom level"])
     :side "top"
     :as-child true}
    [:button.button.flex.items-center.justify-center.px-2.font-mono
     [views/icon "chevron-up"]]]
   [:> DropdownMenu/Portal
    (->> [:zoom/set-50
          :zoom/set-100
          :zoom/set-200
          :separator
          :zoom/focus-selected
          :zoom/fit-selected
          :zoom/fill-selected]
         (map action.views/entity)
         (map #(dissoc % :icon))
         (map views/dropdown-menu-item)
         (into [:> DropdownMenu/Content
                {:class "menu-content rounded-sm"
                 :side "top"
                 :align "end"
                 :on-key-down #(.stopPropagation %)
                 :on-escape-key-down #(.stopPropagation %)}
                [views/dropdownmenu-arrow]]))]])

(defn set-zoom
  [e v]
  (let [new-v (-> (.. e -target -value) js/parseFloat (/ 100))]
    (if (js/isNaN new-v)
      (set! (.. e -target -value) v)
      (rf/dispatch [::frame.events/set-zoom new-v]))))

(defn zoom-decimal-points
  [zoom]
  (condp > zoom
    1 2
    10 1
    0))

(defn zoom-input
  [zoom]
  (let [precision (zoom-decimal-points zoom)
        value (utils.length/->fixed (* 100 zoom) precision false)]
    [:input.text-right.font-mono.p-1
     {:key zoom
      :aria-label (i18n.views/t [::zoom "Zoom"])
      :type "number"
      :input-mode "decimal"
      :min "1"
      :max "10000"
      :style {:width "60px"
              :font-size "inherit"
              :appearance "textfield"}
      :default-value value
      :on-blur #(set-zoom % value)
      :on-key-down #(utils.key/down-handler! % value set-zoom % value)
      :on-wheel #(rf/dispatch (if (pos? (.-deltaY %))
                                [::frame.events/zoom-out]
                                [::frame.events/zoom-in]))}]))

(defn zoom-button-group
  [zoom]
  (let [zoom-out-action (action.views/entity :zoom/out)
        zoom-in-action (action.views/entity :zoom/in)]
    [views/button-group
     (-> zoom-out-action
         (assoc :icon "minus")
         views/action-icon-button)
     (-> zoom-in-action
         (assoc :icon "plus")
         views/action-icon-button)
     [:div.flex.hidden.items-center
      {:class "xl:flex"
       :dir "ltr"}
      [zoom-input zoom]
      [:div.px-2.flex.items-center.font-mono "%"]]
     [zoom-menu]]))

(defn radio-button
  [{:keys [icon class]
    :as action}]
  [:> Tooltip/Root
   [:> Tooltip/Trigger
    {:as-child true}
    [:span
     [views/radio-icon-button icon (action.views/checked? action)
      {:class class
       :aria-label (action.views/label action)
       :on-click (action.views/dispatch action)}]]]
   [:> Tooltip/Portal
    [:> Tooltip/Content
     {:class "tooltip-content"
      :sideOffset 5
      :side "top"
      :on-escape-key-down #(.stopPropagation %)}
     [:div.flex.gap-2.items-center
      (action.views/label action)
      (when @(rf/subscribe [::window.subs/xl?])
        [views/shortcuts action])]]]])

(defn color-picker
  [props & children]
  (let [sm? @(rf/subscribe [::window.subs/sm?])]
    [:> Popover/Root {:modal true}
     (into [:> Popover/Trigger {:as-child true}]
           children)
     [:> Popover/Portal
      [:> Popover/Content
       {:class "popover-content max-w-fit"
        :align "start"
        :side "top"
        :align-offset (:align-offset props)
        :on-escape-key-down #(.stopPropagation %)}
       [:div.p-2
        {:dir "ltr"
         :tab-index 0}
        (if sm?
          [:> PhotoshopPicker props]
          [:> ChromePicker props])]
       [views/popover-arrow]]]]))

(defn color-selectors []
  (let [fill @(rf/subscribe [::document.subs/fill])
        stroke @(rf/subscribe [::document.subs/stroke])
        get-hex #(:hex (js->clj % :keywordize-keys true))]
    [:div.flex
     {:class "gap-0.5"}
     [color-picker
      {:color fill
       :on-change-complete #(rf/dispatch [::element.events/set-attr :fill
                                          (get-hex %)])
       :on-change #(rf/dispatch [::document.events/preview-attr :fill
                                 (get-hex %)])}

      [:button.button.border.border-border.button-size.rounded
       {:title (i18n.views/t [::fill-color "Pick fill color"])
        :style {:background fill}}]]

     [views/icon-button
      "swap-horizontal"
      {:class "bg-transparent!"
       :title (i18n.views/t [::swap-color "Swap fill with stroke"])
       :on-click #(rf/dispatch [::document.events/swap-colors])}]

     [color-picker
      {:color stroke
       :on-change-complete #(rf/dispatch [::element.events/set-attr
                                          :stroke
                                          (get-hex %)])
       :on-change #(rf/dispatch [::document.events/preview-attr
                                 :stroke
                                 (get-hex %)])}
      [:button.relative.border.border-border.button-size.rounded-sm
       {:title (i18n.views/t [::stroke-color "Pick stroke color"])
        :style {:background stroke}}
       [:div.bg-primary.absolute.border.border-border.rounded-xs
        {:class "w-1/2 h-1/2 bottom-1/4 right-1/4"}]]]]))

(defn root []
  (let [md? @(rf/subscribe [::window.subs/md?])
        zoom @(rf/subscribe [::document.subs/zoom])]
    [views/toolbar
     {:class "bg-primary relative justify-center md:justify-start py-2 md:py-1
              gap-2 md:gap-1"}
     [color-selectors]
     [:div.grow.hidden.md:block]
     (when md?
       [:<>
        (->> [:panel/toggle-xml
              :panel/toggle-timeline
              :panel/toggle-history]
             (map action.views/entity)
             (map radio-button)
             (into [:<>]))
        [:div.v-divider]])
     (->> [:view/toggle-grid
           :view/toggle-rulers]
          (map action.views/entity)
          (map radio-button)
          (into [:<>]))
     [snap.views/root]
     [zoom-button-group zoom]
     [coordinates]
     [timeline.views/time-bar]]))
