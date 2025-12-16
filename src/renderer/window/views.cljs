(ns renderer.window.views
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [re-frame.core :as rf]
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
  [system-abbr {:keys [id label action checked abbr]}]
  [:> DropdownMenu/CheckboxItem
   {:class "menu-checkbox-item inset"
    :on-select #(rf/dispatch action)
    :checked @(rf/subscribe checked)}
   [:> DropdownMenu/ItemIndicator
    {:class "menu-item-indicator"}
    [views/icon "checkmark"]]
   [:div (i18n.views/t label)]
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
  [{:keys [icon action class title]}]
  [:button.button.px-3.outline-inset.uppercase.font-mono
   {:class class
    :title title
    :on-click #(rf/dispatch action)}
   [views/icon icon]])

(defn window-control-buttons
  []
  (let [maximized @(rf/subscribe [::window.subs/maximized?])]
    [{:action [::window.events/minimize]
      :title (i18n.views/t [::minimize "Minimize"])
      :icon "window-minimize"}
     {:action [::window.events/toggle-maximized]
      :title (if maximized
               (i18n.views/t [::restore "Restore"])
               (i18n.views/t [::maximize "Maximize"]))
      :icon (if maximized
              "window-restore"
              "window-maximize")}
     {:action [::window.events/close]
      :title (i18n.views/t [::close "Close"])
      :icon "window-close"}]))

(defn app-icon []
  [:div.drag.shrink-0.px-1
   [:img.h-4.w-4
    {:src "img/icon-no-bg.svg"
     :alt "logo"}]])

(defn app-header
  []
  (let [fullscreen? @(rf/subscribe [::window.subs/fullscreen?])
        theme-mode @(rf/subscribe [::theme.subs/computed-mode])
        mac? @(rf/subscribe [::app.subs/mac?])
        web? @(rf/subscribe [::app.subs/web?])
        desktop? @(rf/subscribe [::app.subs/desktop?])
        installable? @(rf/subscribe [::app.subs/installable?])
        title-bar @(rf/subscribe [::document.subs/title-bar])
        md? @(rf/subscribe [::window.subs/md?])
        system-code @(rf/subscribe [::i18n.subs/system-lang-code])
        code @(rf/subscribe [::i18n.subs/lang-code])]
    [:div.flex.relative.items-center
     [:div.px-1.gap-1.flex.items-center.shrink-0
      (when-not (or fullscreen? mac?)
        [app-icon])
      (when (or md? desktop?)
        [:div.flex.relative.bg-secondary
         {:class (when (and mac? (not fullscreen?)) "ml-16")}
         [menubar.views/root]])]
     [:div.drag.grow.items-center
      {:class "pointer-events-none truncate lg:absolute lg:justify-center px-1
               lg:left-1/2 lg:-translate-x-1/2 z-[-1] lg:flex"
       :dir "ltr"}
      title-bar]
     [:div.flex.h-full.flex-1.drag]
     [:div.flex
      [:div.flex.gap-px
       (when installable?
         [views/icon-button "download"
          {:title (i18n.views/t [::install "Install"])
           :class "rounded-none outline-inset bg-transparent!"
           :on-click #(rf/dispatch [::app.events/install])}])
       (when (or md? mac?)
         [:<>
          [dropdown
           [:button.button
            {:title (i18n.views/t [::menubar.views/language "Language"])
             :class "flex gap-1 items-center px-3 uppercase bg-primary font-mono
                     outline-inset"}
            code]
           (->> (menubar.views/languages-submenu)
                (mapv (partial language-item system-code)))]
          [dropdown
           [:button.button
            {:title (i18n.views/t [::menubar.views/theme-mode "Theme Mode"])
             :class "flex gap-1 items-center px-3 bg-primary outline-inset"}
            [views/icon (name theme-mode)]]
           (->> menubar.views/theme-mode-submenu
                (mapv views/dropdown-menu-item))]])
       (when (or fullscreen? mac? (and web? md?))
         [button {:action [::window.events/toggle-fullscreen]
                  :title (if fullscreen?
                           (i18n.views/t [::exit-fullscreen "Exit fullscreen"])
                           (i18n.views/t [::enter-fullscreen
                                          "Enter fullscreen"]))
                  :icon (if fullscreen? "arrow-minimize" "arrow-maximize")
                  :class "bg-primary"}])
       (when (and desktop? (not (or fullscreen? mac?)))
         (->> (window-control-buttons)
              (map button)
              (into [:div.flex])))]]]))
