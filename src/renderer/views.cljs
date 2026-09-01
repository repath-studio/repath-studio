(ns renderer.views
  "A collection of stateless reusable ui components.
   Avoid using subscriptions to keep the components pure."
  (:require
   ["@codemirror/language" :refer [syntaxHighlighting defaultHighlightStyle]]
   ["@codemirror/state" :refer [Compartment]]
   ["@codemirror/theme-one-dark" :refer [oneDark]]
   ["@codemirror/view" :refer [EditorView basicSetup]]
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
   [reagent.core :as reagent]
   [renderer.action.views :as action.views]
   [renderer.i18n.views :as i18n.views]
   [renderer.icon.views :as icon.views]
   [renderer.utils.extra :refer [rpartial]]
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
  (clj->js {"&" {:backgroundColor "transparent"
                 :fontSize "var(--text-xs)"}
            ".cm-content" {:color "var(--foreground-default)"
                           :caretColor "var(--foreground-hovered)"}
            "&.cm-focused" {:outline "none"}
            ".cm-gutters" {:backgroundColor "var(--primary)"
                           :color "var(--foreground-muted)"
                           :border "none"}}))

(defn cm-editor
  [value {:keys [extensions theme-mode on-blur on-change on-keyup on-keydown]}]
  (let [cm (reagent/atom nil)
        ref (react/createRef)
        updating? (atom false)
        theme-compartment (Compartment.)
        theme (fn [mode] (if (= mode :dark) oneDark #js []))
        default-extensions [(.theme EditorView cm-theme)
                            (.of theme-compartment (theme theme-mode))
                            (.-lineWrapping EditorView)
                            (syntaxHighlighting defaultHighlightStyle)
                            (.domEventHandlers EditorView
                                               #js {:keydown on-keydown
                                                    :keyup on-keyup
                                                    :blur on-blur
                                                    :change on-change})]]
    (reagent/create-class
     {:component-did-mount
      (fn [_this]
        (let [dom-el (.-current ref)
              view (EditorView.
                    (clj->js {:doc value
                              :extensions (cond-> (into default-extensions
                                                        basicSetup)

                                            extensions
                                            (conj extensions))

                              :parent dom-el}))]
          (reset! cm view)
          (.dispatch @cm #js {:changes #js {:from 0
                                            :to (.. ^js @cm -state -doc -length)
                                            :insert value}})))

      :component-did-update
      (fn [this _]
        (let [value (second (reagent/argv this))
              options (last (reagent/argv this))
              {:keys [theme-mode]} options]
          (when (and @cm (not= (.. @cm -state -doc toString) value))
            (reset! updating? true)
            (.dispatch @cm #js {:changes #js {:from 0
                                              :to (.. ^js @cm
                                                      -state -doc -length)
                                              :insert value}})
            (reset! updating? false)
            #_(let [last-line (.lastLine @cm)
                    last-ch (count (.getLine ^js @cm last-line))]
                (.setCursor ^js @cm last-line last-ch)))
          (.dispatch @cm #js {:effects (.reconfigure theme-compartment
                                                     (theme theme-mode))})))

      :reagent-render
      (fn []
        [:div {:ref ref}])})))

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
              "h-[30dvh] overflow-hidden gap-px"]
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
