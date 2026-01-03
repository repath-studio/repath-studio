(ns renderer.timeline.views
  (:require
   ["@radix-ui/react-select" :as Select]
   ["@xzdarcy/react-timeline-editor" :refer [Timeline]]
   ["react" :as react]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.element.events :as-alias element.events]
   [renderer.i18n.views :as i18n.views]
   [renderer.panel.subs :as-alias panel.subs]
   [renderer.timeline.events :as-alias timeline.events]
   [renderer.timeline.subs :as-alias timeline.subs]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(def speed-options
  [{:id :0.25
    :value 0.25
    :label "0.25x"}
   {:id :0.5
    :value 0.5
    :label "0.5x"}
   {:id :normal
    :value 1
    :label "1x"}
   {:id :1.5
    :value 1.5
    :label "1.5x"}
   {:id :2
    :value 2
    :label "2x"}])

(defn speed-select
  [editor-ref]
  (let [speed @(rf/subscribe [::timeline.subs/speed])]
    [:div.inline-flex.items-center.gap-2
     [:label.hidden.md:block
      {:for "animation-speed"}
      (i18n.views/t [::speed "Speed"])]
     [:> Select/Root
      {:value speed
       :onValueChange #(.setPlayRate (.-current editor-ref) %)}
      [:> Select/Trigger
       {:class "button px-2 bg-overlay rounded-sm"
        :id "animation-speed"
        :aria-label (i18n.views/t [::no-filter "No filter"])}
       [:> Select/Value
        {:placeholder (i18n.views/t [::filter "Filter"])}
        [:div.flex.gap-1.justify-between.items-center
         {:style {:min-width "50px"}}
         [:span (str speed "x")]
         [:> Select/Icon
          {:class "select-icon"}
          [views/icon "chevron-down"]]]]]
      [:> Select/Portal
       [:> Select/Content
        {:class "menu-content rounded-sm select-content"
         :style {:min-width "auto"}
         :on-escape-key-down #(.stopPropagation %)}
        [:> Select/ScrollUpButton
         {:class "select-scroll-button"}
         [views/icon "chevron-up"]]
        [:> Select/Viewport
         {:class "select-viewport"}
         [:> Select/Group
          (for [{:keys [id value label]} speed-options]
            ^{:key id}
            [:> Select/Item
             {:value value
              :class "menu-item px-2!"}
             [:> Select/ItemText label]])]]
        [:> Select/ScrollDownButton
         {:class "select-scroll-button"}
         [views/icon "chevron-down"]]]]]]))

(defn snap-controls
  []
  (let [grid-snap? @(rf/subscribe [::timeline.subs/grid-snap?])
        guide-snap? @(rf/subscribe [::timeline.subs/guide-snap?])]
    [:div.grow.flex.gap-1
     [views/switch
      (i18n.views/t [::grid-snap "Grid snap"])
      {:id "grid-snap"
       :default-checked grid-snap?
       :on-checked-change #(rf/dispatch [::timeline.events/set-grid-snap %])}]
     [views/switch
      (i18n.views/t [::guide-snap "Guide snap"])
      {:id "guide-snap"
       :default-checked guide-snap?
       :on-checked-change #(rf/dispatch [::timeline.events/set-guide-snap
                                         %])}]]))

(defn toolbar
  [timeline-ref]
  (let [tm @(rf/subscribe [::timeline.subs/time])
        time-formatted @(rf/subscribe [::timeline.subs/time-formatted])
        paused? @(rf/subscribe [::timeline.subs/paused?])
        replay? @(rf/subscribe [::timeline.subs/replay?])
        end @(rf/subscribe [::timeline.subs/end])
        speed @(rf/subscribe [::timeline.subs/speed])
        sm? @(rf/subscribe [::window.subs/sm?])]
    [views/toolbar
     {:class "bg-primary"}
     [views/icon-button "go-to-start"
      {:on-click #(.setTime (.-current timeline-ref) 0)
       :disabled (zero? tm)}]
     [views/radio-icon-button (if paused? "play" "pause") (not paused?)
      {:title (if paused?
                (i18n.views/t [::play "Play"])
                (i18n.views/t [::pause "Pause"]))
       :class (when (pos? tm) "border border-accent")
       :on-click #(if paused?
                    (do (.setPlayRate (.-current timeline-ref) speed)
                        (.play (.-current timeline-ref) #js {:autoEnd true}))
                    (.pause (.-current timeline-ref)))}]
     [views/icon-button "go-to-end"
      {:on-click #(.setTime (.-current timeline-ref) end)
       :disabled (>= tm end)}]
     [views/radio-icon-button "refresh" replay?
      {:title (i18n.views/t [::replay "Replay"])
       :on-click #(rf/dispatch [::timeline.events/toggle-replay])}]
     [speed-select timeline-ref]
     [:span.font-mono.px-2 time-formatted]
     (when sm?
       [:<>
        [:span.v-divider]
        [snap-controls]])]))

(defn register-listeners
  [timeline-ref]
  (doseq
   [[e f]
    [["play"
      #(rf/dispatch-sync [::timeline.events/play])] ;; Prevent navigation
     ["paused"
      #(rf/dispatch-sync [::timeline.events/pause])]
     ["afterSetTime"
      #(rf/dispatch-sync [::timeline.events/set-time (.-time %)])]
     ["setTimeByTick"
      #(rf/dispatch-sync [::timeline.events/set-time (.-time %)])]
     ["afterSetPlayRate"
      #(rf/dispatch [::timeline.events/set-speed (.-rate %)])]
     ["ended"
      #(do (.setTime (.-current timeline-ref) 0)
           (when @(rf/subscribe [::timeline.subs/replay?])
             (.play (.-current timeline-ref) #js {:autoEnd true})))]]]
    (.on (.-listener (.-current timeline-ref)) e f)))

(defn custom-renderer
  [action _row]
  (reagent/as-element
   [:span (.-name action)]))

(defn timeline
  [timeline-ref]
  (let [data @(rf/subscribe [::timeline.subs/rows])
        effects @(rf/subscribe [::timeline.subs/effects])
        grid-snap? @(rf/subscribe [::timeline.subs/grid-snap?])
        guide-snap? @(rf/subscribe [::timeline.subs/guide-snap?])]
    [:> Timeline
     {:editor-data data
      :effects effects
      :ref timeline-ref
      :grid-snap grid-snap?
      :drag-line guide-snap?
      :auto-scroll true
      :getActionRender custom-renderer
      :on-click-action #(let [el-id (keyword (.. %2 -action -id))]
                          (rf/dispatch [::element.events/select
                                        el-id false]))}]))

(defn time-bar
  []
  (let [tm @(rf/subscribe [::timeline.subs/time])
        end @(rf/subscribe [::timeline.subs/end])
        timeline? @(rf/subscribe [::panel.subs/visible? :timeline])]
    [:div.h-px.block.absolute.bottom-0.left-0
     {:style {:width (str (* (/ tm end) 100) "%")
              :background (when-not (or (zero? tm) (zero? end) timeline?)
                            "var(--color-accent)")}}]))

(defn root
  []
  (let [timeline-ref (react/createRef)]
    (reagent/create-class
     {:component-did-mount
      (fn []
        (rf/dispatch [::timeline.events/pause])
        (rf/dispatch [::timeline.events/set-time 0])
        (register-listeners timeline-ref))

      :component-will-unmount
      #(.offAll (.-listener (.-current timeline-ref)))

      :reagent-render
      (fn []
        [:div.flex-col.h-full.w-full
         [toolbar timeline-ref]
         [timeline timeline-ref]])})))
