(ns renderer.views
  "A collection of stateless reusable ui components.
   Avoid using subscriptions to keep the components pure."
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-hover-card" :as HoverCard]
   ["@radix-ui/react-popover" :as Popover]
   ["@radix-ui/react-scroll-area" :as ScrollArea]
   ["@radix-ui/react-select" :as Select]
   ["@radix-ui/react-slider" :as Slider]
   ["@radix-ui/react-switch" :as Switch]
   ["@repath-studio/react-color" :refer [ChromePicker PhotoshopPicker]]
   ["codemirror" :as codemirror]
   ["codemirror/addon/display/placeholder.js"]
   ["codemirror/addon/hint/css-hint.js"]
   ["codemirror/addon/hint/show-hint.js"]
   ["codemirror/mode/css/css.js"]
   ["codemirror/mode/xml/xml.js"]
   ["react" :as react]
   ["react-svg" :refer [ReactSVG]]
   ["sonner" :refer [Toaster]]
   ["tailwind-merge" :refer [twMerge]]
   ["vaul" :refer [Drawer]]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.app.subs :as-alias app.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.utils.key :as utils.key]
   [renderer.window.subs :as-alias window.subs]))

(defn merge-with-class
  [& props]
  (-> (apply merge props)
      (assoc :class (->> (map :class props)
                         (flatten)
                         (apply twMerge)))))

(defn icon
  [icon-name props]
  [:> ReactSVG
   (merge-with-class {:class "flex justify-center [&_svg]:fill-current"
                      :src (str "icons/" icon-name ".svg")
                      :loading #(reagent/as-element
                                 [:div {:class "w-[17px] h-[17px]"}])}
                     props)])

(defn kbd
  [k]
  [:span {:class "p-1 text-2xs bg-overlay rounded-sm font-bold
                  text-foreground-muted uppercase"} k])

(defn icon-button
  [icon-name props]
  [:button
   (merge-with-class {:class "button rounded-sm"} props)
   [icon icon-name]])

(defn loading-indicator
  []
  [icon "spinner" {:class "animate-spin"}])

(defn switch
  [label props]
  [:div.inline-flex.items-center.gap-2
   [:label.bg-transparent
    {:for (:id props)}
    label]
   [:> Switch/Root
    (merge-with-class
     {:class "bg-overlay relative rounded-full w-10 h-6
              data-[state=checked]:bg-accent data-disabled:opacity-50"
      :dir "ltr"}
     props)
    [:> Switch/Thumb
     {:class "block bg-primary rounded-full shadow-sm w-5 h-5
              will-change-transform transition-transform translate-x-0.5
              data-[state=checked]:translate-x-[18px]"}]]])

(defn slider
  [props]
  [:> Slider/Root
   (merge-with-class
    {:class "relative flex items-center select-none w-full touch-none h-full"
     :on-pointer-move #(.stopPropagation %)}
    props)
   [:> Slider/Track {:class "relative h-1 bg-secondary flex-1"}
    [:> Slider/Range {:class "absolute h-full bg-foreground-muted"}]]
   [:> Slider/Thumb {:class "flex shadow-sm h-5 w-2 rounded-xs
                             bg-foreground-hovered
                             data-disabled:bg-foreground-muted"
                     :aria-label (i18n.views/t [::resize-thumb
                                                "Resize panel thumb"])}]])

(defn format-shortcut
  [[shortcut]]
  (into [:div.flex.gap-1.items-center {:dir "ltr"}]
        (comp (map kbd)
              (interpose [:span "+"]))
        (cond-> []
          (:ctrlKey shortcut)
          (conj "Ctrl")

          (:shiftKey shortcut)
          (conj "⇧")

          (:altKey shortcut)
          (conj "Alt")

          :always
          (conj (utils.key/code->key (:keyCode shortcut))))))

(defn shortcuts
  [event]
  (let [event-shortcuts @(rf/subscribe [::app.subs/event-shortcuts event])
        xl? @(rf/subscribe [::window.subs/xl?])]
    (when (and (seq event-shortcuts) xl?)
      (into [:span.inline-flex.text-foreground-muted {:class "gap-1.5"}]
            (comp (map format-shortcut)
                  (interpose [:span]))
            event-shortcuts))))

(defn radio-icon-button
  [icon-name active props]
  [icon-button icon-name
   (merge-with-class {:class ["active:overlay" (when active "accent")]}
                     props)])

(defn context-menu-item
  [{:keys [label action checked disabled]
    :as props}]
  (case (:type props)
    :separator
    [:> ContextMenu/Separator {:class "menu-separator"}]

    :checkbox
    [:> ContextMenu/CheckboxItem
     {:class "menu-checkbox-item inset"
      :onSelect #(rf/dispatch action)
      :checked checked
      :disabled disabled}
     [:> ContextMenu/ItemIndicator
      {:class "menu-item-indicator"}
      [icon "checkmark"]]
     [:div (i18n.views/t label)]
     [shortcuts action]]

    [:> ContextMenu/Item
     {:class "menu-item context-menu-item"
      :onSelect #(rf/dispatch action)
      :disabled disabled}
     [:div (i18n.views/t label)]
     [shortcuts action]]))

(defn dropdown-menu-item
  [{:keys [label action checked]
    :as props}]
  (case (:type props)
    :separator
    [:> DropdownMenu/Separator {:class "menu-separator"}]

    :checkbox
    [:> DropdownMenu/CheckboxItem
     {:class "menu-checkbox-item inset"
      :on-click #(.stopPropagation %)
      :on-select #(rf/dispatch action)
      :checked @(rf/subscribe checked)}
     [:> DropdownMenu/ItemIndicator
      {:class "menu-item-indicator"}
      [icon "checkmark"]]
     [:div (i18n.views/t label)]
     [shortcuts action]]

    [:> DropdownMenu/Item
     {:class "menu-item dropdown-menu-item"
      :onSelect #(rf/dispatch action)}
     (when (:icon props)
       [icon (:icon props)
        {:class "menu-item-indicator"}])
     [:div (i18n.views/t label)]
     [shortcuts action]]))

(defn scroll-area
  [& more]
  (let [children (if (map? (first more)) (rest more) more)]
    [:> ScrollArea/Root
     {:class "overflow-hidden w-full"}
     (into [:> ScrollArea/Viewport
            {:ref (:ref (first more))
             :class "w-full h-full [&>div]:block!"}] children)

     [:> ScrollArea/Scrollbar
      {:class "flex touch-none p-0.5 select-none w-2.5"
       :orientation "vertical"}
      [:> ScrollArea/Thumb
       {:class "relative flex-1 bg-overlay rounded-full"}]]

     [:> ScrollArea/Scrollbar
      {:class "flex touch-none p-0.5 select-none flex-col h-2.5"
       :orientation "horizontal"}
      [:> ScrollArea/Thumb
       {:class "relative flex-1 bg-overlay rounded-full"}]]

     [:> ScrollArea/Corner]]))

(defn popover-arrow []
  [:> Popover/Arrow {:class "fill-primary stroke-border"}])

(defn hovercard-arrow []
  [:> HoverCard/Arrow {:class "fill-primary stroke-border"}])

(defn dropdownmenu-arrow []
  [:> DropdownMenu/Arrow {:class "fill-primary stroke-border"}])

(defn select-arrow []
  [:> Select/Arrow {:class "fill-primary stroke-border"}])

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
       [popover-arrow]]]]))

(def cm-defaults
  {:lineNumbers false
   :matchBrackets true
   :lineWrapping true
   :styleActiveLine true
   :tabMode "spaces"
   :autofocus false
   :extraKeys {"Ctrl-Space" "autocomplete"}
   :theme "tomorrow-night-eighties"
   :autoCloseBrackets true})

(defn cm-render-line
  "Line up wrapped text with the base indentation.
   https://codemirror.net/demo/indentwrap.html"
  [editor line dom-el]
  (let [tab-size (.getOption editor "tabSize")
        off (* (.countColumn codemirror (.-text line) nil tab-size)
               (.defaultCharWidth editor))]
    (set! (.. dom-el -style -textIndent)
          (str "-" off "px"))
    (set! (.. dom-el -style -paddingLeft)
          (str (+ 4 off) "px"))))

(defn cm-editor
  [value {:keys [attrs options on-init on-blur]}]
  (let [cm (reagent/atom nil)
        ref (react/createRef)]
    (reagent/create-class
     {:component-did-mount
      (fn [_this]
        (let [dom-el (.-current ref)
              options (clj->js (merge cm-defaults options))]
          (reset! cm (.fromTextArea codemirror dom-el options))
          (.setValue @cm value)
          (.on @cm "renderLine" cm-render-line)
          (.on @cm "keydown" (fn [_editor evt] (.stopPropagation evt)))
          (.on @cm "keyup" (fn [_editor evt] (.stopPropagation evt)))
          (.refresh @cm)
          (when on-blur (.on @cm "blur" #(on-blur (.getValue %))))
          (when on-init (on-init @cm))))

      :component-will-unmount
      #(when @cm (reset! cm nil))

      :component-did-update
      (fn [this _]
        (let [value (second (reagent/argv this))
              options (:options (last (reagent/argv this)))]
          (.setValue @cm value)
          (doseq [[k v] options]
            (.setOption @cm (name k) v))))

      :reagent-render
      (fn [value]
        [:textarea (merge {:value value
                           :on-blur #()
                           :on-change #()
                           :ref ref} attrs)])})))

(defn drawer
  [{:keys [label direction content disabled snap-points]
    :as attrs}]
  (reagent/with-let [snap (reagent/atom nil)]
    [:> Drawer.Root
     (cond-> {:direction direction}
       snap-points
       (assoc :snapPoints (clj->js snap-points)
              :activeSnapPoint @snap
              :setActiveSnapPoint (fn [v] (reset! snap v))))
     [:> Drawer.Trigger
      {:class "button p-1 rounded h-auto flex flex-col flex-1 text-2xs gap-1
               overflow-hidden items-center"
       :disabled disabled}
      [icon (:icon attrs)]
      [:span.truncate.w-full (i18n.views/t label)]]
     [:> Drawer.Portal
      [:> Drawer.Overlay
       {:class "backdrop"}]
      [:> Drawer.Content
       {:class ["inset-0 fixed z-0 outline-none bg-primary flex shadow-lg"
                (case direction
                  "left"
                  (cond->
                   "right-auto max-w-[80dvw] min-w-[60dvw] py-safe pl-safe"
                    @(rf/subscribe [::app.subs/mac?]) (str " pt-8"))

                  "right"
                  "left-auto max-w-[80dvw] min-w-[60dvw] py-safe pr-safe"

                  "bottom"
                  "top-auto max-h-[60dvh] min-h-[30dvh] px-safe pb-safe"

                  "top"
                  "bottom-auto max-h-[60dvh] min-h-[30dvh] px-safe pt-safe")]
        :style {:margin (cond
                          (or (= direction "left")
                              (= direction "right"))
                          "- env(safe-area-inset-top) 0
                           - env(safe-area-inset-bottom) 0"

                          :else
                          "0 - env(safe-area-inset-right)
                           0 - env(safe-area-inset-left)")}}
       [:> Drawer.Title {:class "sr-only"} (i18n.views/t label)]
       [:> Drawer.Description
        {:as-child true}
        [:div.flex.flex-1.overflow-hidden
         {:class (when (= direction "bottom") "w-full")}
         content]]]]]))

(defn toaster
  [theme]
  [:> Toaster
   {:theme theme
    :toastOptions {:classNames {:toast "bg-primary! border! border-border!
                                        shadow-md! p-4! rounded-md!"
                                :title "text-foreground-hovered!"
                                :description "text-foreground! text-xs"}}
    :icons {:success
            (reagent/as-element [icon "success" {:class "text-success"}])
            :error
            (reagent/as-element [icon "error" {:class "text-error"}])
            :warning
            (reagent/as-element [icon "warning" {:class "text-warning"}])
            :info
            (reagent/as-element [icon "info"])}}])

(defn toolbar
  [& more]
  (let [has-props (map? (first more))
        children (if has-props (rest more) more)
        props (if has-props (first more) {})]
    (into [:div (merge-with-class {:class "flex gap-1 p-1 items-center"} props)]
          children)))

(defn button-group
  [& children]
  (into [:div {:class "flex *:rounded-sm *:border *:border-border
                       *:outline-inset
                       [&>*:not(:first-child)]:rounded-l-none
                       [&>*:not(:last-child)]:border-r-0
                       [&>*:not(:last-child)]:rounded-r-none

                       rtl:[&>*:first-child]:rounded-r-sm!
                       rtl:[&>*:first-child]:border-r!
                       rtl:[&>*:last-child]:rounded-l-sm!

                       rtl:[&>*:not(:last-child)]:rounded-l-none
                       rtl:[&>*:not(:first-child)]:border-r-0
                       rtl:[&>*:not(:first-child)]:rounded-r-none"}]
        children))

(defn help
  [message]
  [:div.absolute.top-0.left-0.w-full.pointer-events-none
   [:div.hidden.justify-center.w-full.p-4.lg:flex
    [:div.bg-primary.overflow-hidden.shadow.rounded-full
     [:div.text-xs.gap-1.flex.flex-wrap.py-2.px-4.justify-center.truncate
      {:aria-live "polite"}
      message]]]])
