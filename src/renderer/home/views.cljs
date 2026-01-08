(ns renderer.home.views
  (:require
   ["@radix-ui/react-select" :as Select]
   ["path-browserify" :as path-browserify]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.db :as db]
   [renderer.dialog.events :as-alias dialog.events]
   [renderer.document.events :as-alias document.events]
   [renderer.events :as-alias events]
   [renderer.i18n.views :as i18n.views]
   [renderer.views :as views]))

(defn document-size-select []
  [:> Select/Root
   {:onValueChange #(let [size (get db/a-series-paper-sizes %)]
                      (rf/dispatch [::document.events/new-from-template size]))}
   [:> Select/Trigger
    {:class "button px-2 bg-overlay rounded-sm"
     :aria-label (i18n.views/t [::select-size "Select size"])}
    [:div.flex.items-center.gap-2
     [:> Select/Value
      {:placeholder (i18n.views/t [::select-template "Select template"])}]
     [:> Select/Icon
      [views/icon "chevron-down"]]]]
   [:> Select/Portal
    [:> Select/Content
     {:class "menu-content rounded-sm select-content"
      :style {:min-width "auto"}
      :on-escape-key-down #(.stopPropagation %)}
     [:> Select/Viewport
      {:class "select-viewport"}
      [:> Select/Group
       [:> Select/Item
        {:value :empty-canvas
         :class "menu-item px-2!"}
        [:> Select/ItemText
         (i18n.views/t [::empty-canvas "Empty canvas"])]]
       (for [[k _v] (sort db/a-series-paper-sizes)]
         ^{:key k}
         [:> Select/Item
          {:value k
           :class "menu-item px-2!"}
          [:> Select/ItemText (str "A" k)]])]]]]])

(defn recent-document
  [{:keys [path title]
    :as recent}]
  [:div.flex.items-center.gap-x-2.flex-wrap
   [views/icon "folder"]
   [:button.button-link.text-lg
    {:on-click #(rf/dispatch [::document.events/open-recent recent])}
    (or title (.basename path-browserify path))]
   (when path
     [:span.text-lg.text-foreground-muted (.dirname path-browserify path)])])

(def help-commands
  [["command"
    [::command-panel "Command panel"]
    [::dialog.events/show-cmdk]]
   ["earth"
    [::website "Website"]
    [::events/open-remote-url "https://repath.studio/"]]
   ["commit"
    [::source-code "Source Code"]
    [::events/open-remote-url "https://github.com/repath-studio/repath-studio"]]
   ["list"
    [::changelog "Changelog"]
    [::events/open-remote-url "https://repath.studio/roadmap/changelog/"]]])

(defn help-command
  [icon label event]
  [:div.flex.items-center.gap-2.flex-wrap
   [views/icon icon]
   [:button.button-link.text-lg
    {:on-click #(rf/dispatch event)}
    (i18n.views/t label)]
   [views/shortcuts event]])

(defn root
  [recent-documents]
  [:div.flex.overflow-hidden
   [views/scroll-area
    [:div.flex.sm:justify-center.p-2
     [:div.justify-around.flex
      [:div.flex.w-full
       [:div.flex-1
        [:div.p-4.lg:p-12
         [:img.h-24.w-24.mb-3
          {:src "img/icon-no-bg.svg"
           :alt "logo"}]
         [:h1.text-4xl.mb-1.font-light config/app-name]

         [:p.text-xl.text-foreground-muted.font-bold
          (i18n.views/t [::svg-description "Vector Graphics Editor"])]

         [:h2.mb-3.mt-8.text-2xl (i18n.views/t [::start "Start"])]

         [:div.flex.items-center.gap-2.flex-wrap
          [views/icon "file"]
          [:button.button-link.text-lg
           {:on-click #(rf/dispatch [::document.events/new])}
           (i18n.views/t [::new "New"])]
          [views/shortcuts [::document.events/new]]

          [:span (i18n.views/t [::or "or"])]

          [document-size-select]]

         [:div.flex.items-center.gap-2
          [views/icon "folder"]
          [:button.button-link.text-lg
           {:on-click #(rf/dispatch [::document.events/open])}
           (i18n.views/t [::open "Open"])]
          [views/shortcuts [::document.events/open]]]

         (when (seq recent-documents)
           [:<> [:h2.mb-3.mt-8.text-2xl
                 (i18n.views/t [::recent "Recent"])]

            (for [recent (take 5 recent-documents)]
              ^{:key (:id recent)}
              [recent-document recent])])

         [:h2.mb-3.mt-8.text-2xl
          (i18n.views/t [::help "Help"])]

         (->> help-commands
              (map #(apply help-command %))
              (into [:div]))]]]]]]])
