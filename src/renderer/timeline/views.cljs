(ns renderer.timeline.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["@radix-ui/react-select" :as Select]
   ["@xzdarcy/react-timeline-editor" :refer [Timeline]]
   ["react" :as react]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.action.views :as action.views]
   [renderer.element.events :as-alias element.events]
   [renderer.i18n.views :as i18n.views]
   [renderer.panel.subs :as-alias panel.subs]
   [renderer.panel.views :as panel.views]
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

(defn toolbar
  [timeline-ref]
  (let [tm @(rf/subscribe [::timeline.subs/time])
        time-formatted @(rf/subscribe [::timeline.subs/time-formatted])
        paused? @(rf/subscribe [::timeline.subs/paused?])
        end @(rf/subscribe [::timeline.subs/end])
        speed @(rf/subscribe [::timeline.subs/speed])
        sm? @(rf/subscribe [::window.subs/sm?])
        md? @(rf/subscribe [::window.subs/md?])]
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
     [views/tooltip-action-icon-button :timeline/toggle-replay]
     [speed-select timeline-ref]
     [:span.font-mono.px-2 time-formatted]
     (when sm?
       [:<>
        [:span.v-divider]
        [:div.grow.flex.gap-1
         [views/action-switch :timeline/toggle-grid-snap]
         [views/action-switch :timeline/toggle-guide-snap]]])
     [:div.flex-1]
     (when md? [panel.views/close-button :timeline])]))

(defn register-listeners
  [timeline-ref]
  (doseq
   [[e f]
    [["play"
      #(rf/dispatch-sync [::timeline.events/play])]
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

(defn params->id
  [params]
  (uuid (.. params -action -id)))

(defn timeline
  [timeline-ref]
  (let [data @(rf/subscribe [::timeline.subs/rows])
        effects @(rf/subscribe [::timeline.subs/effects])
        grid-snap? @(rf/subscribe [::timeline.subs/grid-snap?])
        guide-snap? @(rf/subscribe [::timeline.subs/guide-snap?])
        preview-action! #(rf/dispatch-sync [::timeline.events/preview-action
                                            (params->id %)
                                            (.-start %)
                                            (.-end %)])
        finalize-action! #(rf/dispatch-sync [::timeline.events/finalize-action
                                             (.-start %)
                                             (.-end %)])]
    [:> ContextMenu/Root
     [:> ContextMenu/Trigger
      {:class "flex h-full w-full overflow-hidden"}
      [:> Timeline
       {:editor-data data
        :effects effects
        :ref timeline-ref
        :grid-snap grid-snap?
        :drag-line guide-snap?
        :auto-scroll true
        :getActionRender custom-renderer
        :onActionResizing preview-action!
        :onActionResizeEnd finalize-action!
        :onActionMoving preview-action!
        :onActionMoveEnd finalize-action!
        :onClickActionOnly (fn [e params]
                             (.stopPropagation e)
                             (rf/dispatch [::element.events/select
                                           (params->id params)
                                           (.-shiftKey e)]))}]]
     [:> ContextMenu/Portal
      (->> [:object/locking
            :object/entity]
           (map (comp :actions action.views/deref-action-group))
           (interpose {:type :separator})
           (flatten)
           (map views/context-menu-item)
           (into [:> ContextMenu/Content
                  {:class "menu-content context-menu-content"
                   :on-key-down #(.stopPropagation %)
                   :on-escape-key-down #(.stopPropagation %)}]))]]))

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
        [:div.flex.flex-col.h-full.w-full.overflow-hidden.gap-px
         [toolbar timeline-ref]
         [timeline timeline-ref]])})))
