(ns renderer.window.views
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [re-frame.core :as rf]
   [renderer.action.views :as action.views]
   [renderer.app.events :as-alias app.events]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.i18n.subs :as-alias i18n.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.menubar.views :as menubar.views]
   [renderer.theme.subs :as-alias theme.subs]
   [renderer.views :as views]
   [renderer.window.events :as-alias window.events]
   [renderer.window.subs :as-alias window.subs]))

(defn language-item
  [system-abbr {:keys [id abbr]
                :as action}]
  [:> DropdownMenu/CheckboxItem
   {:class "menu-checkbox-item inset"
    :on-select (action.views/dispatch action)
    :checked (action.views/checked? action)}
   [:> DropdownMenu/ItemIndicator
    {:class "menu-item-indicator"}
    [views/icon "checkmark"]]
   [:div [action.views/label action]]
   (if (= id "system")
     [:span.font-mono.text-foreground-disabled (or system-abbr "EN")]
     [:span.font-mono.text-foreground-muted abbr])])

(defn dropdown
  [trigger-content dropdown-items]
  [:> DropdownMenu/Root
   [:> DropdownMenu/Trigger
    {:as-child true}
    trigger-content]
   [:> DropdownMenu/Portal
    (into [:> DropdownMenu/Content
           {:side "bottom"
            :align "end"
            :position "popper"
            :class "menu-content rounded-sm select-content"
            :on-key-down #(.stopPropagation %)
            :on-escape-key-down #(.stopPropagation %)}
           [views/dropdownmenu-arrow]]
          dropdown-items)]])

(defn button
  [{:keys [icon event class title]}]
  [:button.button.px-3.outline-inset.uppercase.font-mono
   {:class class
    :title title
    :on-click #(rf/dispatch event)}
   [views/icon icon]])

(defn window-control-buttons
  []
  (let [maximized @(rf/subscribe [::window.subs/maximized?])]
    [{:event [::window.events/minimize]
      :title (i18n.views/t [::minimize "Minimize"])
      :icon "window-minimize"}
     {:event [::window.events/toggle-maximized]
      :title (if maximized
               (i18n.views/t [::restore "Restore"])
               (i18n.views/t [::maximize "Maximize"]))
      :icon (if maximized
              "window-restore"
              "window-maximize")}
     {:event [::window.events/close]
      :title (i18n.views/t [::close "Close"])
      :icon "window-close"}]))

(defn app-icon []
  [:div.drag.shrink-0.px-1
   [:img.h-4.w-4
    {:src "img/icon-no-bg.svg"
     :alt "logo"}]])

(defn fullscreen-toggle
  [enabled]
  [button {:event [::window.events/toggle-fullscreen]
           :title (if enabled
                    (i18n.views/t [::exit-fullscreen "Exit fullscreen"])
                    (i18n.views/t [::enter-fullscreen
                                   "Enter fullscreen"]))
           :icon (if enabled "arrow-minimize" "arrow-maximize")
           :class "bg-primary"}])

(defn install-button []
  [views/icon-button "download"
   {:title (i18n.views/t [::install "Install"])
    :class "rounded-none outline-inset bg-transparent!"
    :on-click #(rf/dispatch [::app.events/install])}])

(defn titlebar [s]
  [:div.drag.grow.items-center
   {:dir "ltr"
    :class "pointer-events-none truncate lg:absolute lg:justify-center px-1
            lg:left-1/2 lg:-translate-x-1/2 z-[-1] lg:flex"}
   s])

(defn language-select
  [system-code code]
  [dropdown
   [:button.button
    {:title (i18n.views/t [::menubar.views/language "Language"])
     :class "flex gap-1 items-center px-3 uppercase bg-primary font-mono
             outline-inset"}
    code]
   (->> @(rf/subscribe [::i18n.subs/language-actions])
        (mapv (partial language-item system-code)))])

(defn theme-mode-select
  [mode]
  (let [{:keys [label actions]} (action.views/deref-action-group :theme/mode)]
    [dropdown
     [:button.button
      {:title (i18n.views/t label)
       :class "flex gap-1 items-center px-3 bg-primary outline-inset"}
      [views/icon (name mode)]]
     (->> actions
          (mapv views/dropdown-menu-item))]))

(defn window-controls []
  (->> (window-control-buttons)
       (map button)
       (into [:div.flex])))

(defn right-controls []
  (let [fullscreen? @(rf/subscribe [::window.subs/fullscreen?])
        theme-mode @(rf/subscribe [::theme.subs/computed-mode])
        mac? @(rf/subscribe [::app.subs/mac?])
        installable? @(rf/subscribe [::app.subs/installable?])
        md? @(rf/subscribe [::window.subs/md?])
        system-code @(rf/subscribe [::i18n.subs/system-lang-code])
        code @(rf/subscribe [::i18n.subs/lang-code])
        window-controls? @(rf/subscribe [::window.subs/window-controls?])
        fullscreen-toggle? @(rf/subscribe [::window.subs/fullscreen-toggle?])]
    [:div.flex.gap-px
     (when installable? [install-button])
     (when (or md? mac?)
       [:<>
        [language-select system-code code]
        [theme-mode-select theme-mode]])
     (when fullscreen-toggle? [fullscreen-toggle fullscreen?])
     (when window-controls? [window-controls])]))

(defn app-header []
  (let [title-bar @(rf/subscribe [::document.subs/title-bar])
        fullscreen? @(rf/subscribe [::window.subs/fullscreen?])
        mac? @(rf/subscribe [::app.subs/mac?])
        app-icon? @(rf/subscribe [::window.subs/app-icon?])
        menubar? @(rf/subscribe [::window.subs/menubar?])]
    [:div.flex.relative.items-center
     [:div.px-1.gap-1.flex.items-center.shrink-0
      (when app-icon? [app-icon])
      (when menubar?
        [:div.flex.relative.bg-secondary
         {:class (when (and mac? (not fullscreen?)) "ml-16")}
         [menubar.views/root]])]
     [titlebar title-bar]
     [:div.flex.h-full.flex-1.drag]
     [right-controls]]))
