(ns renderer.frame.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["react" :as react]
   ["react-frame-component" :default Frame :refer [useFrame]]
   [config :as config]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [reagent.dom.server :as server]
   [renderer.action.views :as action.views]
   [renderer.app.subs :as-alias app.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.frame.events :as-alias frame.events]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.input.impl.wheel :as input.impl.wheel]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.dom :as utils.dom]
   [renderer.utils.math :as utils.math]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(def thumb-thickness (/ config/track-thickness 2))

(defn axis-delta
  [orientation delta]
  (if (= orientation :horizontal)
    [delta 0]
    [0 delta]))

(defn pan-by
  [ratio orientation delta-px]
  (rf/dispatch [::frame.events/pan-by
                (axis-delta orientation (/ delta-px ratio))]))

(defn thumb
  [orientation offset size margin]
  [:div
   {:class ["absolute rounded-full transition-opacity cursor-default!"
            "opacity-50 hover:opacity-80 active:opacity-80"]
    :style (if (= orientation :horizontal)
             {:left (+ offset config/track-thickness)
              :width size
              :top 0
              :padding margin
              :height "100%"}
             {:top (+ offset config/track-thickness)
              :height size
              :left 0
              :padding margin
              :width "100%"})}
   [:div {:class "rounded-full bg-backdrop h-full w-full cursor-default!"}]])

(defn on-pointer-down
  [orientation {:keys [size offset ratio track]} dom-rect]
  (fn [e]
    (.setPointerCapture (.-currentTarget e) (.-pointerId e))
    (when (identical? (.-target e) (.-currentTarget e))
      (let [horizontal? (= orientation :horizontal)
            position (if horizontal?
                       (- (.-clientX e) (:left dom-rect))
                       (- (.-clientY e) (:top dom-rect)))
            target-offset (utils.math/clamp (- position (/ size 2))
                                            0
                                            (- track size))]
        (pan-by ratio orientation (- target-offset offset))))))

(defn on-pointer-move
  [orientation {:keys [offset size ratio track]}]
  (fn [e]
    (when (.hasPointerCapture (.-currentTarget e) (.-pointerId e))
      (let [move (if (= orientation :horizontal)
                   (.-movementX e)
                   (.-movementY e))
            at-start? (<= offset 0)
            at-end? (>= offset (- track size))
            skip? (or (zero? move)
                      (and at-start? (neg? move))
                      (and at-end? (pos? move)))]
        (when-not skip?
          (pan-by ratio orientation move))))))

(defn prevent-resize
  "Prevent pointer events on overlapping panel separators that handle pointer
   events on document level."
  [e]
  (when (.closest (.-target e) "[data-scrollbar]")
    (.preventDefault e)))

(defn scrollbar
  [_orientation _options]
  (reagent/create-class
   {:component-did-mount
    #(doseq [event-type #js ["pointerdown" "pointermove" "pointerup"]]
       (.addEventListener js/window event-type prevent-resize true))

    :component-will-unmount
    #(doseq [event-type #js ["pointerdown" "pointermove" "pointerup"]]
       (.removeEventListener js/window event-type prevent-resize true))

    :reagent-render
    (fn [orientation options]
      (let [margin (/ (- config/track-thickness thumb-thickness) 2)
            {:keys [size offset]} options
            dom-rect @(rf/subscribe [::app.subs/dom-rect])]
        [:div.absolute.transition-colors.hover:bg-white.hover:cursor-default!
         {:data-scrollbar true
          :style (if (= orientation :horizontal)
                   {:bottom 0
                    :left 0
                    :height config/track-thickness
                    :right 0}
                   {:top 0
                    :right 0
                    :width config/track-thickness
                    :bottom 0})
          :on-pointer-down (on-pointer-down orientation options dom-rect)
          :on-pointer-move (on-pointer-move orientation options)}
         [thumb orientation offset size margin]]))}))

(defn scrollbars
  []
  (let [h-scroll @(rf/subscribe [::frame.subs/scrollbar :horizontal])
        v-scroll @(rf/subscribe [::frame.subs/scrollbar :vertical])]
    [:<>
     [scrollbar :horizontal h-scroll]
     [scrollbar :vertical v-scroll]]))

(defn inner-component
  "We need access to the iframe's window to add the wheel listener.
   This is required in order to prevent the default zoom behavior.
   https://github.com/ryanseddon/react-frame-component#accessing-the-iframes-window-and-document
   https://github.com/reagent-project/reagent/blob/master/doc/ReactFeatures.md#function-components"
  []
  (let [frame-window (.-window (useFrame))]
    (reagent/create-class
     {:component-did-mount
      #(.addEventListener frame-window
                          "wheel"
                          input.impl.wheel/handler!
                          #js {:passive false})

      :component-will-unmount
      #(.removeEventListener frame-window
                             "wheel"
                             input.impl.wheel/handler!)

      :reagent-render #()})))

(defn initial-markup
  "The iframe is isolated, so we cannot access the css vars of the parent.
   https://github.com/ryanseddon/react-frame-component#initialcontent"
  []
  [:html {:data-theme "light"}
   [:head [:link {:rel "stylesheet"
                  :href "./css/default-theme.css"}]]
   [:body {:style {:width "100%"
                   :height "100%"
                   :overflow "hidden"
                   :user-select "none"
                   :touch-action "none"
                   :background "var(--secondary)"
                   :font-family "var(--sans)"
                   :font-size "var(--font-size)"
                   :margin 0
                   :font-synthesis "none"}}]])

(def resize-observer
  (js/ResizeObserver.
   (fn [entries]
     (let [dom-rect (-> (first entries)
                        (.. -target getBoundingClientRect toJSON)
                        (js->clj :keywordize-keys true))]
       ;; We dispatch synchronously to use the dom-rect in the next render.
       (rf/dispatch-sync [::frame.events/resize dom-rect])))))

(def context-menu-action-groups
  [:edit/clipboard
   :object/grouping
   :object/locking
   :object/animate
   :object/entity])

(defn on-keyboard-event
  "Propagates the keyboard events to the parent window."
  [e]
  (.preventDefault e)
  (.dispatchEvent js/window.parent.document
                  (js/KeyboardEvent. (.-type e) e)))

(defn root
  "Our canvas is wrapped within an iframe element that hosts anything
   that needs to be rendered.
   https://github.com/ryanseddon/react-frame-component
   https://medium.com/@ryanseddon/rendering-to-iframes-in-react-d1cb92274f86"
  []
  (let [ref (react/createRef)]
    (reagent/create-class
     {:component-did-mount
      #(.observe resize-observer (.-current ref))

      :component-will-unmount
      #(.disconnect resize-observer)

      :reagent-render
      (fn []
        (let [root-el @(rf/subscribe [::element.subs/root])
              idle? @(rf/subscribe [::tool.subs/idle?])
              md? @(rf/subscribe [::window.subs/md?])
              {:keys [x y]} @(rf/subscribe [::app.subs/dom-rect])]
          [:<>
           [:> Frame
            {:initial-content (server/render-to-static-markup (initial-markup))
             :mount-target "body"
             :class "overflow-hidden flex-1 border-0"
             :on-key-down on-keyboard-event
             :on-key-up on-keyboard-event
             :id utils.dom/frame-id
             :title (i18n.views/t [::main-canvas "main canvas"])
             :ref ref
             :style {:background (-> root-el :attrs :fill)}}
            [:f> inner-component]
            [:> ContextMenu/Root
             [:> ContextMenu/Trigger
              {:disabled (not idle?)}
              [element.hierarchy/render root-el]]
             [:> ContextMenu/Portal
              (->> context-menu-action-groups
                   (map (comp :actions action.views/deref-action-group))
                   (interpose {:type :separator})
                   (flatten)
                   (map views/context-menu-item)
                   (into [:> ContextMenu/Content
                          {:class "menu-content context-menu-content"
                           :on-close-auto-focus #(.preventDefault %)
                           :on-key-down #(.stopPropagation %)
                           :on-escape-key-down #(.stopPropagation %)
                           :style {:margin-left (str x "px")
                                   :margin-top (str y "px")}}]))]]]
           (when md? [scrollbars])]))})))
