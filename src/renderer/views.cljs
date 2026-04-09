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
   ["codemirror" :as codemirror]
   ["codemirror/addon/display/placeholder.js"]
   ["codemirror/addon/hint/css-hint.js"]
   ["codemirror/addon/hint/show-hint.js"]
   ["codemirror/mode/css/css.js"]
   ["codemirror/mode/xml/xml.js"]
   ["react" :as react]
   ["sonner" :refer [Toaster]]
   ["tailwind-merge" :refer [twMerge]]
   [reagent.core :as reagent]
   [renderer.action.views :as action.views]
   [renderer.i18n.views :as i18n.views]
   [renderer.icon.views :as icon.views]
   [renderer.utils.key :as utils.key]))

(defn merge-with-class
  [& props]
  (-> (apply merge props)
      (assoc :class (->> (map :class props)
                         (flatten)
                         (apply twMerge)))))

(defn icon
  [id props]
  (when-let [path (icon.views/path id)]
    [:svg (merge-with-class {:class "fill-current"
                             :viewBox "0 0 17 17"
                             :width "17"
                             :height "17"}
                            props)
     path]))

(defn kbd
  [k]
  [:span {:class "p-1 text-2xs bg-overlay rounded-sm font-bold uppercase
                  text-foreground-muted"} k])

(defn icon-button
  [icon-name props]
  [:button
   (merge-with-class {:class "button flex justify-center rounded-sm
                              items-center"}
                     props)
   [icon icon-name]])

(defn action-icon-button
  [action & {:as attrs}]
  [icon-button (:icon action)
   (merge {:disabled (action.views/disabled? action)
           :aria-label (action.views/label action)
           :on-click (action.views/dispatch action)}
          attrs)])

(defn action-button
  [action-id & {:as props}]
  (when-let [action (action.views/deref-action action-id)]
    [:button
     (merge-with-class {:class "button"
                        :disabled (action.views/disabled? action)
                        :on-click (action.views/dispatch action)}
                       props)
     [action.views/label action]]))

(defn loading-indicator []
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
  [shortcut]
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
  [action]
  (let [event-shortcuts (:shortcuts action)]
    (when (seq event-shortcuts)
      (into [:span.text-foreground-muted.hidden.lg:inline-flex
             {:class "gap-1.5"}]
            (comp (map format-shortcut)
                  (interpose [:span]))
            event-shortcuts))))

(defn radio-icon-button
  [icon-name active props]
  [icon-button icon-name
   (merge-with-class {:class ["active:overlay" (when active "accent")]}
                     props)])

(defn context-menu-item
  [action]
  (cond
    (= (:type action) :separator)
    [:> ContextMenu/Separator {:class "menu-separator"}]

    (:active action)
    [:> ContextMenu/CheckboxItem
     {:class "menu-checkbox-item inset"
      :onSelect (action.views/dispatch action)
      :checked (action.views/checked? action)
      :disabled (action.views/disabled? action)}
     [:> ContextMenu/ItemIndicator
      {:class "menu-item-indicator"}
      [icon "checkmark"]]
     [:div [action.views/label action]]
     [shortcuts action]]

    :else
    [:> ContextMenu/Item
     {:class "menu-item context-menu-item"
      :onSelect (action.views/dispatch action)
      :disabled (action.views/disabled? action)}
     [:div [action.views/label action]]
     [shortcuts action]]))

(defn dropdown-menu-item
  [action]
  (cond
    (= :separator (:type action))
    [:> DropdownMenu/Separator {:class "menu-separator"}]

    (:active action)
    [:> DropdownMenu/CheckboxItem
     {:class "menu-checkbox-item inset"
      :on-click #(.stopPropagation %)
      :on-select (action.views/dispatch action)
      :checked (action.views/checked? action)
      :disabled (action.views/disabled? action)}
     [:> DropdownMenu/ItemIndicator
      {:class "menu-item-indicator"}
      [icon "checkmark"]]
     [:div [action.views/label action]]
     [shortcuts action]]

    :else
    [:> DropdownMenu/Item
     {:class "menu-item dropdown-menu-item"
      :onSelect (action.views/dispatch action)
      :disabled (action.views/disabled? action)}
     (when (:icon action)
       [icon (:icon action)
        {:class "menu-item-indicator"}])
     [:div [action.views/label action]]
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
