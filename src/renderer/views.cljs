(ns renderer.views
  "A collection of stateless reusable ui components.
   Avoid using subscriptions to keep the components pure."
  (:require
   ["@codemirror/commands" :refer [defaultKeymap
                                   indentWithTab
                                   insertNewlineAndIndent]]
   ["@codemirror/language" :refer [syntaxHighlighting defaultHighlightStyle]]
   ["@codemirror/state" :refer [Compartment Prec]]
   ["@codemirror/theme-one-dark" :refer [oneDark oneDarkHighlightStyle]]
   ["@codemirror/view" :refer [EditorView basicSetup keymap]]
   ["@lezer/highlight" :refer [highlightCode]]
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-hover-card" :as HoverCard]
   ["@radix-ui/react-popover" :as Popover]
   ["@radix-ui/react-scroll-area" :as ScrollArea]
   ["@radix-ui/react-select" :as Select]
   ["@radix-ui/react-slider" :as Slider]
   ["@radix-ui/react-switch" :as Switch]
   ["@radix-ui/react-tooltip" :as Tooltip]
   ["react" :as react]
   ["sonner" :refer [Toaster]]
   ["tailwind-merge" :refer [twMerge]]
   ["vaul" :refer [Drawer]]
   [clojure.string :as string]
   [reagent.core :as reagent]
   [renderer.action.views :as action.views]
   [renderer.i18n.views :as i18n.views]
   [renderer.icon.views :as icon.views]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.codemirror :as utils.codemirror]
   [renderer.utils.color :as utils.color]
   [renderer.utils.extra :refer [rpartial]]
   [renderer.utils.key :as utils.key]
   [renderer.utils.math :as utils.math]))

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
  [:span {:class ["p-1 text-2xs bg-overlay rounded-sm font-bold uppercase"
                  "text-foreground-muted"]} k])

(defn icon-button
  [icon-name props]
  [:button
   (merge-with-class {:class ["button flex justify-center rounded-sm"
                              "items-center"]}
                     props)
   [icon icon-name]])

(defn tag
  [content on-remove & {:keys [remove-label]}]
  [:div.flex.items-center.gap-2.bg-overlay.rounded.py-1
   {:class "px-1.5"}
   content
   [icon-button "times"
    {:on-click on-remove
     :title (or remove-label (i18n.views/t [::remove "Remove"]))
     :class "button-size-sm text-foreground-muted"}]])

(defn action-icon-button
  [action & {:as props}]
  (when-let [action (action.views/deref-action action)]
    [icon-button (:icon action)
     (merge {:disabled (action.views/disabled? action)
             :title (action.views/label action)
             :on-click (action.views/dispatch action)}
            props)]))

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
     {:class ["bg-overlay relative rounded-full w-10 h-6"
              "data-[state=checked]:bg-accent data-disabled:opacity-50"]
      :dir "ltr"}
     props)
    [:> Switch/Thumb
     {:class ["block bg-primary rounded-full shadow-sm w-5 h-5"
              "will-change-transform transition-transform translate-x-0.5"
              "data-[state=checked]:translate-x-[18px]"]}]]])

(defn action-switch
  [action & {:as props}]
  (when-let [action (action.views/deref-action action)]
    [switch (action.views/label action)
     (merge-with-class
      {:id (:id action)
       :checked (action.views/checked? action)
       :disabled (action.views/disabled? action)
       :on-checked-change (action.views/dispatch action)}
      props)]))

(defn slider
  [props]
  [:> Slider/Root
   (merge-with-class
    {:class "relative flex items-center select-none w-full touch-none h-full"
     :on-pointer-move #(.stopPropagation %)}
    props)
   [:> Slider/Track {:class "relative h-1 bg-secondary flex-1"}
    [:> Slider/Range {:class "absolute h-full bg-foreground-muted"}]]
   [:> Slider/Thumb {:class ["flex shadow-sm h-5 w-2 rounded-xs"
                             "bg-foreground-hovered"
                             "data-disabled:bg-foreground-muted"]
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

          (:keyCode shortcut)
          (conj (utils.key/code->key (:keyCode shortcut))))))

(defn shortcuts
  [action & {:keys [limit]}]
  (let [event-shortcuts (:shortcuts action)]
    (when (seq event-shortcuts)
      (let [truncated? (and limit (> (count event-shortcuts) limit))
            shown (cond->> event-shortcuts limit (take limit))]
        (into [:span.text-foreground-muted.hidden.lg:inline-flex.items-center
               {:class "gap-1.5"}]
              (cond-> (into []
                            (comp (map format-shortcut)
                                  (interpose [:span]))
                            shown)
                truncated? (conj [:span "…"])))))))

(defn radio-icon-button
  [icon-name active props]
  [icon-button icon-name
   (merge-with-class {:class ["active:overlay" (when active "accent")]}
                     props)])

(defn tooltip-icon-button
  [icon-name label props]
  [:> Tooltip/Root
   [:> Tooltip/Trigger
    {:as-child true}
    [:span [icon-button icon-name props]]]
   [:> Tooltip/Portal
    [:> Tooltip/Content
     {:class "tooltip-content pointer-events-none"
      :side-offset 5
      :side "top"
      :on-escape-key-down #(.stopPropagation %)}
     label]]])

(defn tooltip-action-icon-button
  [action & {:as content-props}]
  (when-let [action (action.views/deref-action action)]
    [:> Tooltip/Root
     [:> Tooltip/Trigger
      {:as-child true}
      [:span
       (if (:active action)
         [radio-icon-button (:icon action) (action.views/checked? action)
          {:class (:class action)
           :aria-label (action.views/label action)
           :on-click (action.views/dispatch action)}]
         [action-icon-button action
          {:aria-label (action.views/label action)
           :title nil}])]]
     [:> Tooltip/Portal
      [:> Tooltip/Content
       (merge {:class "tooltip-content pointer-events-none"
               :sideOffset 5
               :side "top"
               :on-escape-key-down #(.stopPropagation %)}
              content-props)
       [:div.flex.gap-2.items-center
        [action.views/label action]
        [shortcuts action]]]]]))

(defn action-button-group
  [action-group & {:as content-props}]
  (->> action-group
       action.views/deref-action-group
       :actions
       (map (rpartial tooltip-action-icon-button content-props))
       (into [:<>])))

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
     [:div.flex.items-center.gap-2
      (when (:icon action)
        [icon (:icon action)])
      [action.views/label action]]
     [shortcuts action]]

    :else
    [:> DropdownMenu/Item
     {:class "menu-item"
      :onSelect (action.views/dispatch action)
      :disabled (action.views/disabled? action)}
     [:div.flex.items-center.gap-2
      (when (:icon action)
        [icon (:icon action)])
      [action.views/label action]]
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

(def cm-theme
  (clj->js {"&"
            {:backgroundColor "transparent"
             :fontSize "var(--text-xs)"}

            ".cm-content"
            {:color "var(--foreground-default)"
             :caretColor "var(--foreground-hovered)"}

            "&.cm-focused"
            {:outline "none"}

            ".cm-gutters"
            {:backgroundColor "var(--primary)"
             :color "var(--foreground-muted)"
             :border "none"}

            "&.cm-focused .cm-matchingBracket"
            {:backgroundColor "transparent"
             :color "var(--foreground-hovered)"}

            "&.cm-focused .cm-nonmatchingBracket"
            {:backgroundColor "transparent"
             :color "var(--foreground-hovered)"}}))

(def cm-defaults
  [(.of keymap defaultKeymap)
   (.of keymap indentWithTab)
   (.-lineWrapping EditorView)
   (syntaxHighlighting defaultHighlightStyle)
   (.of keymap #js {:key "Shift-Enter"
                    :run insertNewlineAndIndent})])

(defn cm-editor
  [value {:keys [extensions theme-mode
                 on-blur on-change on-keyup on-keydown on-paste]}]
  (let [cm (reagent/atom nil)
        updating? (atom false)
        ref (react/createRef)
        theme-compartment (Compartment.)
        theme (fn [mode] (if (= mode :dark) oneDark #js []))
        dynamic-extensions [(.theme EditorView cm-theme)
                            (.of theme-compartment (theme theme-mode))
                            (.of EditorView.updateListener
                                 (fn [^js change]
                                   (when (and on-change
                                              (not @updating?)
                                              (.-docChanged change))
                                     (on-change change))))
                            (.high Prec (.domEventHandlers
                                         EditorView
                                         #js {:keydown on-keydown
                                              :keyup on-keyup
                                              :blur on-blur
                                              :paste on-paste}))]]
    (reagent/create-class
     {:component-did-mount
      (fn [_this]
        (let [dom-el (.-current ref)
              view (EditorView.
                    (clj->js {:doc value
                              :parent dom-el
                              :extensions (cond-> dynamic-extensions
                                            :always (-> (conj cm-defaults)
                                                        (into basicSetup))
                                            extensions (conj extensions))}))]
          (reset! cm view)
          (utils.codemirror/set-value @cm value)))

      :component-did-update
      (fn [this _]
        (let [value (second (reagent/argv this))
              options (last (reagent/argv this))
              {:keys [theme-mode]} options]
          (when (and @cm (not= (.. @cm -state -doc toString) value))
            #_(reset! updating? true)
            (utils.codemirror/set-value @cm value)
            #_(reset! updating? false)
            (.dispatch @cm #js {:selection
                                #js {:anchor (utils.codemirror/get-length
                                              @cm)}}))
          (.dispatch @cm #js {:effects (.reconfigure theme-compartment
                                                     (theme theme-mode))})))

      :reagent-render
      (fn [] [:div {:ref ref}])})))

(defn theme-highlighters
  [theme-mode]
  (if (= theme-mode :light)
    #js [defaultHighlightStyle]
    #js [oneDarkHighlightStyle defaultHighlightStyle]))

(defn highlight-piece
  [i [text class]]
  (cond->> text
    (seq class)
    (into [:span {:key i
                  :class class}])))

(defn static-highlight
  "https://lezer.codemirror.net/examples/highlight/#running-a-highlighter"
  [text theme-mode parser & {:as props}]
  (let [tree (.parse parser text)
        pieces (atom [])]
    (highlightCode text tree (theme-highlighters theme-mode)
                   (fn [piece classes]
                     (swap! pieces conj [(str piece) (str classes)]))
                   (fn [] (swap! pieces conj ["\n" nil])))
    [:pre
     (merge-with-class {:class "p-0 m-0"} props)
     (map-indexed highlight-piece @pieces)]))

(defn toaster
  [theme]
  [:> Toaster
   {:theme theme
    :closeButton true
    :duration js/Infinity
    :toastOptions {:classNames {:toast "bg-primary! border! border-border!
                                        shadow-md! p-4! rounded-md!"
                                :closeButton "right-0! left-auto! bg-primary!
                                              h-6! w-6! transform-none!
                                              translate-x-1/2! -translate-y-1/2!
                                              rounded-sm! border! border-border!
                                              hover:text-foreground-hovered!
                                              text-foreground!"
                                :title "text-foreground-hovered!"
                                :description "text-foreground! text-xs
                                              overflow-y-auto! max-h-20"}}
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
  (into [:div {:class ["flex *:rounded-sm *:border *:border-border"
                       "*:outline-inset"
                       "[&>*:not(:first-child)]:rounded-l-none"
                       "[&>*:not(:last-child)]:border-r-0"
                       "[&>*:not(:last-child)]:rounded-r-none"

                       "rtl:[&>*:first-child]:rounded-r-sm!"
                       "rtl:[&>*:first-child]:border-r!"
                       "rtl:[&>*:last-child]:rounded-l-sm!"

                       "rtl:[&>*:not(:last-child)]:rounded-l-none"
                       "rtl:[&>*:not(:first-child)]:border-r-0"
                       "rtl:[&>*:not(:first-child)]:rounded-r-none"]}]
        children))

(defn drawer
  [props & children]
  [:> Drawer.Root
   {:direction "bottom"
    :modal false}
   [:> Drawer.Trigger
    {:class ["button p-1 rounded h-auto flex flex-col flex-1 text-2xs gap-1"
             "overflow-hidden items-center"]}
    [icon (:icon props)]
    [:span.truncate.w-full (i18n.views/t (:label props))]]
   [:> Drawer.Portal
    [:> Drawer.Content
     {:class ["inset-0 fixed z-0 outline-none bg-secondary flex shadow-lg"
              "flex-col items-center top-auto px-safe pb-safe rounded-t-xl"
              "h-70 overflow-hidden gap-px"]
      :style {:margin "0 - env(safe-area-inset-right)
                       0 - env(safe-area-inset-left)"
              :box-shadow "0 -10px 15px -3px
                           var(--tw-shadow-color, rgb(0 0 0 / 0.1)),
                           0 -4px 6px -4px
                           var(--tw-shadow-color, rgb(0 0 0 / 0.1))"}}
     [:div.bg-primary.w-full
      [:> Drawer.Handle
       {:class "mx-auto my-3! w-12! h-1.5! rounded-full bg-overlay!"}]]
     [:> Drawer.Title
      {:class "sr-only"}
      (i18n.views/t (:label props))]
     (into [:div.flex.flex-1.overflow-hidden.w-full] children)]]])

(defn color-selection-gradient
  [hue]
  (str "linear-gradient(0deg, rgba(0,0,0,1), rgba(0,0,0,0)), "
       "linear-gradient(90deg, rgba(255,255,255,1), rgba(2, 2, 2, 0)), "
       (-> (utils.color/hsl->color hue 1 0.5)
           (utils.color/->css))))

(defn color-selection
  [{:keys [color notation on-change on-commit]}]
  (reagent/with-let [dragging? (atom false)]
    (let [[hue saturation lightness] (.hsl color)
          hue (if (js/isNaN hue) 0 hue)
          alpha (.alpha color)
          top-lightness (fn [x] (+ 0.5 (* 0.5 (- 1 x))))
          position [saturation lightness]
          node (react/createRef)
          update-position
          (fn [event cb]
            (when-let [^js rect (.getBoundingClientRect (.-current node))]
              (let [x (utils.math/clamp (/ (- (.-clientX event) (.-left rect))
                                           (.-width rect))
                                        0 1)
                    y (utils.math/clamp (/ (- (.-clientY event) (.-top rect))
                                           (.-height rect))
                                        0 1)]
                (-> (utils.color/hsl->color hue x (* (top-lightness x) (- 1 y)))
                    (utils.color/set-alpha notation alpha)
                    (cb)))))]
      [:div.relative.size-full.cursor-crosshair.touch-none.h-40
       {:ref node
        :style {:background (color-selection-gradient hue)}
        :on-pointer-down (fn [e]
                           (.preventDefault e)
                           (.setPointerCapture (.-current node) (.-pointerId e))
                           (reset! dragging? true)
                           (update-position e on-change))
        :on-pointer-move (fn [e]
                           (when @dragging?
                             (update-position e on-change)))
        :on-pointer-up (fn [e]
                         (reset! dragging? false)
                         (update-position e on-commit))
        :on-pointer-cancel (fn [e]
                             (reset! dragging? false)
                             (update-position e on-commit))}
       [:div.absolute.h-4.w-4.rounded-full.border-2.border-white
        {:class "-translate-x-1/2 -translate-y-1/2 pointer-events-none"
         :style {:left (str (* 100 (first position)) "%")
                 :top (str (* 100 (- 1 (/ lightness
                                          (top-lightness saturation)))) "%")
                 :box-shadow "0 0 0 1px rgba(0,0,0,0.5)"}}]])))

(def hue-track-colors
  ["#ff0000"
   "#ffff00"
   "#00ff00"
   "#00ffff"
   "#0000ff"
   "#ff00ff"
   "#ff0000"])

(defn hue-slider
  [{:keys [color notation on-change on-commit]}]
  (let [hue (first (.hsl color))]
    [:> Slider/Root
     {:class "relative flex h-4 w-full touch-none px-1 data-disabled:opacity-50"
      :max 359
      :disabled (js/isNaN hue)
      :step 1
      :value [(first (.hsl color))]
      :on-value-change (fn [[v]]
                         (on-change (utils.color/set-hue color notation v)))
      :on-value-commit (fn [[v]]
                         (on-commit (utils.color/set-hue color notation v)))
      :on-pointer-move #(.stopPropagation %)}
     [:> Slider/Track
      {:class "relative my-0.5 h-3 grow"
       :style {:background (str "linear-gradient(90deg, "
                                (string/join ", " hue-track-colors)
                                ")")}}]
     [:> Slider/Thumb
      {:class "block h-4 w-2 bg-foreground-hovered"
       :title (i18n.views/t [::adjust-hue "Adjust hue"])}]]))

(defn alpha-slider
  [{:keys [color notation on-change on-commit]}]
  [:> Slider/Root
   {:class "relative flex h-4 w-full touch-none px-1"
    :max 1
    :step 0.01
    :value [(.alpha color)]
    :on-value-change (fn [[v]]
                       (on-change (utils.color/set-alpha color notation v)))
    :on-value-commit (fn [[v]]
                       (on-commit (utils.color/set-alpha color notation v)))
    :on-pointer-move #(.stopPropagation %)}
   [:> Slider/Track
    {:class "relative my-0.5 h-3 grow"
     :style {:background (str "repeating-conic-gradient(transparent 0 25%,"
                              "var(--foreground-disabled) 0 50%)"
                              "50% / 12px 12px")}}
    [:div.absolute.inset-0
     {:style {:background (str "linear-gradient(90deg, transparent, "
                               (.css (.alpha color 1)) ")")}}]
    [:> Slider/Range
     {:class "absolute h-full bg-transparent"}]]
   [:> Slider/Thumb
    {:class "block h-4 w-2 bg-foreground-hovered"
     :title (i18n.views/t [::adjust-opecity "Adjust opecity"])}]])

(defn eye-dropper-button
  [{:keys [notation on-commit value]}]
  [icon-button "eye-dropper"
   {:class "my-1!"
    :title (i18n.views/t [::pick-color "Pick color"])
    :on-click #(-> (js/EyeDropper.)
                   (.open)
                   (.then (fn [^js result]
                            (some-> (.-sRGBHex result)
                                    (utils.color/string->color value)
                                    (utils.color/->css notation)
                                    (on-commit))))
                   (.catch (fn [_])))}])

(defn color-notation-select
  [{:keys [color notation on-change]}]
  [:> Select/Root
   {:value notation
    :on-value-change (fn [notation]
                       (on-change (utils.color/->css color notation)))}
   [:> Select/Trigger
    {:class "button px-2 rounded-sm shrink-0"
     :title (i18n.views/t [::select-color-type "Select color type"])}
    [:div
     [:> Select/Value ""]
     [:> Select/Icon [icon "chevron-down"]]]]
   [:> Select/Portal
    [:> Select/Content
     {:class "menu-content rounded-sm select-content"
      :on-key-down #(.stopPropagation %)
      :on-escape-key-down #(.stopPropagation %)}
     [:> Select/ScrollUpButton
      {:class "select-scroll-button"}
      [icon "chevron-up"]]
     (->> utils.color/supported-notations
          (map (fn [format]
                 [:> Select/Item
                  {:value format
                   :class "menu-item px-2!"}
                  [:> Select/ItemText
                   (string/upper-case format)]]))
          (into [:> Select/Viewport {:class "select-viewport"}]))
     [:> Select/ScrollDownButton
      {:class "select-scroll-button"}
      [icon "chevron-down"]]]]])

(defn set-color-channel-value
  [e {:keys [value color notation channel on-commit]}]
  (let [raw (.. e -target -value)
        new-value (cond-> raw
                    (not= channel "hex")
                    (js/parseFloat raw))]
    (if-not (utils.color/valid-channel-value? channel raw)
      (set! (.. e -target -value) value)
      (-> (case channel
            "alpha" (.alpha color new-value)
            "hex" (utils.color/string->color new-value value)
            (.set color (str notation "." channel) new-value))
          (utils.color/->css notation)
          (on-commit)))))

(defn color-channel-input
  [{:keys [value index notation]
    :as options}]
  (let [channel (utils.color/index->channel index notation)
        value (cond-> value
                (number? value)
                (utils.attribute/->fixed 2))
        options (merge options {:value value
                                :channel channel})]
    [:div.flex.flex-col.items-center.w-full.text-2xs
     [:input.form-element.text-center.p-0!
      {:id channel
       :default-value value
       :on-blur #(set-color-channel-value % options)
       :on-key-down #(utils.key/down-handler % value
                                             set-color-channel-value options)}]
     [:label.text-foreground-muted.uppercase
      {:for channel}
      channel]]))

(defn color-channels
  [{:keys [color notation]
    :as options}]
  (->> (utils.color/channel-values color notation)
       (map-indexed (fn [index value]
                      ^{:key (str index value)}
                      [color-channel-input (merge options {:value value
                                                           :index index})]))
       (into [:div.flex.w-full.items-center.rounded-sm.gap-1])))

(defn color-picker
  [{:keys [value on-change on-commit dropper]} & children]
  (let [color (utils.color/string->color value)
        notation (utils.color/string->notation (str value))
        options {:color color
                 :notation notation
                 :on-change on-change
                 :on-commit on-commit}]
    (into [:div.flex.flex-col.gap-4.w-70.p-2.bg-primary
           {:dir "ltr"}
           [:div.flex.flex-col.gap-4
            [color-selection options]
            [:div.flex.items-center.gap-1.justify-center
             (when dropper
               [eye-dropper-button options])
             [:div.flex.flex-col.flex-1.space-between.gap-1
              [hue-slider options]
              [alpha-slider options]]]
            [:div.flex.items-center.gap-2
             [color-notation-select options]
             [color-channels options]]]]
          children)))
