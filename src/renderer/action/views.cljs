(ns renderer.action.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-tooltip" :as Tooltip]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.action.subs :as action.subs]
   [renderer.app.subs :as-alias app.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.utils.extra :refer [rpartial]]
   [renderer.utils.key :as utils.key]
   [renderer.views :as views]))

(defn disabled?
  "Returns the disabled html attribute state.
   An action is disabled if it has an :enabled subscription that returns false,
   or if it has an :enabled boolean value of false."
  [{:keys [enabled]}]
  (cond
    (false? enabled)
    true

    (vector? enabled)
    (some-> enabled
            rf/subscribe
            deref
            not)

    :else
    nil))

(defn available?
  "An action is active if it doesn't have an :available subscription,
   or if it has an :available subscription that returns true."
  [action]
  (if-not (:available action)
    true
    (-> action
        :available
        rf/subscribe
        deref)))

(defn checked?
  "Returns the checked html attribute state.
   An action is checked if it has a :checked subscription that returns true."
  [action]
  (some-> action
          :active
          rf/subscribe
          deref))

(defn dispatch
  [action]
  (fn [_e]
    (some-> action
            :event
            rf/dispatch)))

(defn label
  [action]
  (some-> action
          :label
          i18n.views/t))

(defn deref-action
  [action]
  (when-let [action (cond-> action
                      (keyword? action)
                      (->> (conj [::action.subs/action])
                           (rf/subscribe)
                           deref))]
    (when (available? action)
      (let [shortcuts @(rf/subscribe [::action.subs/action-shortcuts
                                      (:id action)])]
        (cond-> action
          (seq shortcuts) (assoc :shortcuts shortcuts)
          (empty? shortcuts) (dissoc :shortcuts))))))

(defn deref-action-group
  [id]
  (when-let [group @(rf/subscribe [::action.subs/action-group id])]
    (update group :actions (partial keep deref-action))))

(def modifier-glyphs
  "Modifier key -> [mac non-mac] display symbols, in display order"
  [[:ctrlKey ["⌃" "Ctrl"]]
   [:shiftKey ["⇧" "Shift"]]
   [:altKey ["⌥" "Alt"]]
   [:metaKey ["⌘" "Meta"]]])

(def key-code-glyphs
  "Key code -> display name, unlisted codes render as their code->key name"
  {"BACKSPACE" "⌫"
   "ENTER" "⏎"
   "WIN_KEY" "⊞"
   "CONTEXT_MENU" "≣"
   "SPACE" "␣"
   "TILDE" "˜"
   "SINGLE_QUOTE" "'"
   "COMMA" ","
   "PERIOD" "."
   "SLASH" "/"
   "DASH" "-"
   "EQUALS" "="
   "PLUS_SIGN" "+"
   "SEMICOLON" ";"
   "UP" "↑"
   "DOWN" "↓"
   "LEFT" "←"
   "RIGHT" "→"
   "FIRST_MEDIA_KEY" "⏮"
   "LAST_MEDIA_KEY" "⏭"
   "ZERO" "0"
   "ONE" "1"
   "TWO" "2"
   "THREE" "3"
   "FOUR" "4"
   "FIVE" "5"
   "SIX" "6"
   "SEVEN" "7"
   "EIGHT" "8"
   "NINE" "9"
   "NUM_ZERO" "NUM_0"
   "NUM_ONE" "NUM_1"
   "NUM_TWO" "NUM_2"
   "NUM_THREE" "NUM_3"
   "NUM_FOUR" "NUM_4"
   "NUM_FIVE" "NUM_5"
   "NUM_SIX" "NUM_6"
   "NUM_SEVEN" "NUM_7"
   "NUM_EIGHT" "NUM_8"
   "NUM_NINE" "NUM_9"
   "OPEN_SQUARE_BRACKET" "["
   "CLOSE_SQUARE_BRACKET" "]"
   "BACKSLASH" "\\"
   "DELETE" "DEL"})

(defn shortcut-glyphs
  [shortcut mac?]
  (concat
   (keep (fn [[modifier [mac other]]]
           (when (modifier shortcut)
             (if mac? mac other)))
         modifier-glyphs)
   (when-let [code (:keyCode shortcut)]
     (let [k (utils.key/code->key code)]
       [(get key-code-glyphs k k)]))))

(defn format-shortcut
  [action-id shortcut]
  (let [web-platform @(rf/subscribe [::app.subs/web-platform])
        mac? (= web-platform "Mac")
        default? @(rf/subscribe [::action.subs/default-shortcut?
                                 action-id
                                 shortcut])]
    (into [:div.flex.gap-1.items-center
           {:dir "ltr"
            :class (when-not default? "text-foreground-hovered")}]
          (comp (map #(views/kbd % {:class (when-not default?
                                             "text-foreground-hovered")}))
                (interpose [:span "+"]))
          (shortcut-glyphs shortcut mac?))))

(defn shortcut-keys
  [action-id v truncated?]
  (cond->> v
    :always
    (into [] (comp (map (partial format-shortcut action-id))
                   (interpose [:span])))

    truncated?
    (conj [:span "…"])

    :always
    (into [:span {:class ["text-foreground-muted hidden lg:inline-flex"
                          "flex-wrap items-center gap-2"]}])))

(defn shortcuts
  [action & {:keys [limit]}]
  (let [event-shortcuts (:shortcuts action)]
    (when (seq event-shortcuts)
      (let [truncated? (and limit (> (count event-shortcuts) limit))
            shown (cond->> event-shortcuts limit (take limit))]
        [shortcut-keys (:id action) shown truncated?]))))

(defn context-menu-item
  [action]
  (cond
    (= (:type action) :separator)
    [:> ContextMenu/Separator {:class "menu-separator"}]

    (:active action)
    [:> ContextMenu/CheckboxItem
     {:class "menu-checkbox-item inset"
      :onSelect (dispatch action)
      :checked (checked? action)
      :disabled (disabled? action)}
     [:> ContextMenu/ItemIndicator
      {:class "menu-item-indicator"}
      [views/icon "checkmark"]]
     [:div [label action]]
     [shortcuts action]]

    :else
    [:> ContextMenu/Item
     {:class "menu-item context-menu-item"
      :onSelect (dispatch action)
      :disabled (disabled? action)}
     [:div [label action]]
     [shortcuts action]]))

(defn dropdown-menu-item
  [action & {:as props}]
  (cond
    (= :separator (:type action))
    [:> DropdownMenu/Separator (views/merge-with-class {:class "menu-separator"}
                                                       props)]

    (:active action)
    [:> DropdownMenu/CheckboxItem
     (views/merge-with-class {:class "menu-checkbox-item inset"
                              :on-click #(.stopPropagation %)
                              :on-select (dispatch action)
                              :checked (checked? action)
                              :disabled (disabled? action)}
                             props)
     [:> DropdownMenu/ItemIndicator
      {:class "menu-item-indicator"}
      [views/icon "checkmark"]]
     [:div.flex.items-center.gap-2
      (when (:icon action)
        [views/icon (:icon action)])
      [label action]]
     [shortcuts action]]

    :else
    [:> DropdownMenu/Item
     (views/merge-with-class {:class "menu-item"
                              :onSelect (dispatch action)
                              :disabled (disabled? action)}
                             props)
     [:div.flex.items-center.gap-2
      (when (:icon action)
        [views/icon (:icon action)])
      [label action]]
     [shortcuts action]]))

(defn icon-button
  [action & {:as props}]
  (when-let [action (deref-action action)]
    [views/icon-button (:icon action)
     (merge {:disabled (disabled? action)
             :title (label action)
             :on-click (dispatch action)}
            props)]))

(defn radio-icon-button
  [action & {:as props}]
  (when-let [action (deref-action action)]
    [icon-button action
     (views/merge-with-class {:class ["active:overlay"
                                      (when (checked? action) "accent")]}
                             props)]))

(defn switch
  [action & {:as props}]
  (when-let [action (deref-action action)]
    [views/switch (label action)
     (views/merge-with-class
      {:id (:id action)
       :checked (checked? action)
       :disabled (disabled? action)
       :on-checked-change (dispatch action)}
      props)]))

(defn tooltip-icon-button
  [action & {:as content-props}]
  (when-let [action (deref-action action)]
    [:> Tooltip/Root
     [:> Tooltip/Trigger
      {:as-child true}
      [:span
       (if (:active action)
         [views/radio-icon-button (:icon action) (checked? action)
          {:class (:class action)
           :aria-label (label action)
           :on-click (dispatch action)}]
         [icon-button action
          {:aria-label (label action)
           :title nil}])]]
     [:> Tooltip/Portal
      [:> Tooltip/Content
       (merge {:class "tooltip-content pointer-events-none"
               :sideOffset 5
               :side "top"
               :on-escape-key-down #(.stopPropagation %)}
              content-props)
       [:div.flex.gap-2.items-center
        [label action]
        [shortcuts action]]]]]))

(defn button-group
  [action-group & {:as content-props}]
  (->> action-group
       deref-action-group
       :actions
       (map (rpartial tooltip-icon-button content-props))
       (into [:<>])))

(defn button-with-options
  [action options-action-group & children]
  (let [action (deref-action action)
        options (deref-action-group options-action-group)]
    (reagent/with-let [open (reagent/atom false)]
      [:button.button.rounded-sm.items-center.gap-1.justify-items-center
       (views/merge-with-class
        {:title (label action)
         :disabled (disabled? action)
         :class ["active:bg-overlay shrink-0 px-1 flex"
                 (when (checked? action) "accent")
                 (when @open "bg-overlay!")]
         :on-click (dispatch action)})
       [views/icon (:icon action)]
       [:> DropdownMenu/Root
        {:on-open-change #(reset! open %)}
        [:> DropdownMenu/Trigger
         {:as-child true}
         [:div.h-full.flex.items-center.hover:pb-1
          {:class "min-h-[inherit]"
           :role "button"
           :title (i18n.views/t (:label options))}
          [views/icon "chevron-up"]]]
        [:> DropdownMenu/Portal
         (->> (concat (map dropdown-menu-item (:actions options)) children)
              (into [:> DropdownMenu/Content
                     {:side "top"
                      :align "end"
                      :sideOffset 5
                      :alignOffset -5
                      :position "popper"
                      :class "menu-content rounded-sm select-content"
                      :on-click #(.stopPropagation %)
                      :on-key-down #(.stopPropagation %)
                      :on-escape-key-down #(.stopPropagation %)}
                     [views/dropdownmenu-arrow]]))]]])))
