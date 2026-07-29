(ns renderer.dialog.views
  (:require
   ["@radix-ui/react-dialog" :as Dialog]
   ["cmdk" :as Command]
   [clojure.string :as string]
   [config :as config]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.action.events :as-alias action.events]
   [renderer.action.subs :as-alias action.subs]
   [renderer.action.views :as action.views]
   [renderer.app.subs :as-alias app.subs]
   [renderer.dialog.events :as-alias dialog.events]
   [renderer.dialog.subs :as-alias dialog.subs]
   [renderer.document.events :as-alias document.events]
   [renderer.i18n.views :as i18n.views]
   [renderer.menubar.views :as menubar.views]
   [renderer.utils.key :as utils.key]
   [renderer.views :as views]))

(defn button
  [{:keys [event label auto-focus class]}]
  [:button.button.px-1.rounded.font-medium.w-full.bg-overlay.sm:bg-transparent
   {:class class
    :auto-focus auto-focus
    :on-click #(rf/dispatch [::dialog.events/close event])}
   (i18n.views/t label)])

(defn button-bar
  [& children]
  (into [:div.flex.flex-wrap.gap-3.sm:flex-nowrap.sm:gap-2] children))

(defn about
  []
  (let [user-agent @(rf/subscribe [::app.subs/user-agent])]
    [:div
     [:p
      [:img.w-full.grayscale {:src "img/banner.png"
                              :alt "Repath Studio banner"}]]
     [:p.grid.grid-cols-2.gap-3
      {:style {:grid-template-columns "auto 1fr"}}
      [:strong (i18n.views/t [::version "Version:"])]
      [:code config/version]
      [:strong (i18n.views/t [::browser "Browser:"])]
      [:code user-agent]]
     [button-bar
      [button {:label [::ok "OK"]
               :auto-focus true
               :class "accent"}]]]))

(defn confirmation
  [{:keys [content confirm-event confirm-label cancel-event
           cancel-label]}]
  [:div
   (cond
     (nil? content)
     [:p (i18n.views/t [::action-cannot-undone
                        "This action cannot be undone."])]

     (string? content)
     [:p content]

     :else content)

   [button-bar
    [button {:label (or cancel-label [::cancel "Cancel"])
             :event cancel-event}]
    [button {:label (or confirm-label [::ok "OK"])
             :event confirm-event
             :auto-focus true
             :class "accent"}]]])

(defn save
  [{:keys [id title]}]
  [:div
   (i18n.views/t
    [::changes-will-be-lost
     [:p "Your changes to %1 will be lost if you close the document without
          saving."]]
    [[:strong title]])
   [button-bar
    [button {:label [::dont-save "Don't save"]
             :event [::document.events/close id false]}]
    [button {:label [::cancel "Cancel"]}]
    [button {:label [::save "Save"]
             :auto-focus true
             :class "accent"
             :event [::document.events/save {:id id
                                             :close true}]}]]])

(defn cmdk-item
  [{:keys [id label event icon]
    :as action}]
  (when-not (or (= (:type action) :separator)
                (action.views/disabled? action))
    [:> Command/CommandItem
     {:on-select #(rf/dispatch [::dialog.events/close event])
      :class ["flex p-2 rounded-md items-center justify-between group"
              "data-[selected=true]:bg-overlay"]}
     [:div.flex.items-center.gap-2
      [:div.w-7.h-7.rounded.line-height-6.flex.justify-center.items-center
       {:class (when icon "bg-overlay")}
       [views/icon icon]]
      [:div (->> label
                 (remove nil?)
                 (map i18n.views/t)
                 (string/join " - "))]]
     [:div.flex.items-center.gap-2
      [views/shortcuts action]
      [views/icon-button "edit"
       {:aria-label (i18n.views/t [::edit-shortcuts "Edit keyboard shortcuts"])
        :class ["opacity-0 group-hover:opacity-100 focus:opacity-100"
                "h-5 w-5 text-foreground-muted"]
        :on-pointer-down #(.stopPropagation %)
        :on-click (fn [e]
                    (.stopPropagation e)
                    (.preventDefault e)
                    (rf/dispatch [::dialog.events/show-edit-shortcut id]))}]]]))

(defn cmdk-group-inner
  [items label]
  (mapcat (fn [item]
            (if (:actions item)
              (cmdk-group-inner (:actions item) (:label item))
              (when-let [action (cond-> item
                                  (keyword? item)
                                  action.views/deref-action)]
                [[cmdk-item (update action :label #(vector label %))]])))
          items))

(defn cmdk-group
  [{:keys [label actions]}]
  (when (seq actions)
    (->> (cmdk-group-inner actions nil)
         (into [:> Command/CommandGroup
                {:heading (i18n.views/t label)}]))))

(defn cmdk
  []
  [:> Command/Command
   {:label "Command Menu"
    :on-key-down #(.stopPropagation %)}
   [:> Command/CommandInput
    {:class "p-3 bg-primary border-b border-border w-full"
     :placeholder (i18n.views/t [::search-command "Search for a command"])}]
   [views/scroll-area
    (->> (menubar.views/submenus)
         (keep cmdk-group)
         (into [:> Command/CommandList
                {:class "p-1 max-h-[50dvh]"}
                [:> Command/CommandEmpty
                 {:class "p-2"}
                 (i18n.views/t [::no-results "No results found."])]]))]])

(def modifier-key-codes
  "keyCodes for keys that are modifiers on their own and shouldn't be captured
   as a standalone shortcut (Ctrl, Shift, Alt, Meta/Win keys)."
  #{16 17 18 91 92 93 224})

(defn keydown->shortcut
  [e]
  (let [key-code (.-keyCode e)]
    (when-not (contains? modifier-key-codes key-code)
      (cond-> {:keyCode key-code}
        (.-ctrlKey e) (assoc :ctrlKey true)
        (.-shiftKey e) (assoc :shiftKey true)
        (.-altKey e) (assoc :altKey true)))))

(defn shortcut->string
  [shortcut]
  (->> [(when (:ctrlKey shortcut) "Ctrl")
        (when (:shiftKey shortcut) "Shift")
        (when (:altKey shortcut) "Alt")
        (some-> (:keyCode shortcut) utils.key/code->key)]
       (remove nil?)
       (string/join " + ")))

(defn edit-shortcut
  [_id]
  (let [pending (reagent/atom nil)]
    (fn [id]
      (let [shortcuts @(rf/subscribe [::action.subs/action-shortcuts id])
            add! (fn [_]
                   (when @pending
                     (rf/dispatch [::action.events/add-shortcut id @pending]))
                   (reset! pending nil))
            remove! (fn [shortcut]
                      (rf/dispatch
                       [::action.events/remove-shortcut id shortcut]))]
        [:div
         [views/icon-button "times"
          {:aria-label (i18n.views/t [::close "Close"])
           :class "absolute top-3 right-3 h-6 w-6 text-foreground-muted"
           :on-click #(rf/dispatch [::dialog.events/close])}]
         [:div.flex.gap-2.mb-4
          [:input.form-element.p-2.rounded.flex-1.bg-primary.border
           {:class "border-border"
            :auto-focus true
            :read-only true
            :placeholder (i18n.views/t [::press-keys "Press a key combination"])
            :value (if @pending (shortcut->string @pending) "")
            :on-key-down (fn [e]
                           (.preventDefault e)
                           (.stopPropagation e)
                           (when-let [shortcut (keydown->shortcut e)]
                             (reset! pending shortcut)))}]
          [:button.button.px-3.rounded.font-medium.bg-overlay
           {:disabled (nil? @pending)
            :on-click add!}
           (i18n.views/t [::add "Add"])]]
         (into [:div.flex.flex-wrap.gap-2.min-h-8.mb-4]
               (map (fn [shortcut]
                      ^{:key (str shortcut)}
                      [views/tag
                       [views/format-shortcut shortcut]
                       #(remove! shortcut)])
                    shortcuts))
         [button-bar
          [:button.button.px-1.rounded.font-medium.w-full.bg-overlay
           {:class "sm:bg-transparent"
            :on-click #(rf/dispatch [::action.events/reset-shortcuts id])}
           (i18n.views/t [::reset-shortcuts "Reset to defaults"])]
          [button {:label [::ok "OK"]
                   :class "accent"}]]]))))

(defn root
  []
  (let [active-dialog @(rf/subscribe [::dialog.subs/active])
        {:keys [title content attrs]} active-dialog]
    [:> Dialog/Root
     {:open (boolean active-dialog)
      :on-open-change #(rf/dispatch [::dialog.events/close])}
     [:> Dialog/Portal
      [:> Dialog/Overlay
       {:class ["fixed inset-0 flex items-center justify-center bg-backdrop"
                "animate-in fade-in"]}]
      [:> Dialog/Content
       (views/merge-with-class
        {:class ["fixed bg-primary rounded-lg overflow-hidden shadow-xl border"
                 "border-border left-1/2 top-1/2 w-125 max-w-9/10"
                 "-translate-1/2 animate-in zoom-in-95 p-6 m-safe"]
         :on-key-down #(.stopPropagation %)}
        attrs)
       (when title
         [:> Dialog/Title
          {:as-child true}
          (if (string? title)
            [:h2.text-xl.pb-4.text-foreground-hovered title]
            title)])
       [:> Dialog/Description
        {:as-child true}
        [:div content]]]]]))
