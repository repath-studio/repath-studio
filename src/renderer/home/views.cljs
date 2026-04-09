(ns renderer.home.views
  (:require
   ["@radix-ui/react-select" :as Select]
   ["path-browserify" :as path-browserify]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.action.views :as action.views]
   [renderer.db :as db]
   [renderer.document.events :as-alias document.events]
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

(defn command
  [action]
  [:div.flex.items-center.gap-3.flex-wrap
   [views/icon (:icon action)]
   [:button.button-link.text-lg
    {:on-click (action.views/dispatch action)}
    [action.views/label action]]
   [views/shortcuts action]])

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
          [command (action.views/deref-action :document/new)]
          [:span (i18n.views/t [::or "or"])]
          [document-size-select]]

         [command (action.views/deref-action :document/open)]

         (when (seq recent-documents)
           [:<> [:h2.mb-3.mt-8.text-2xl
                 (i18n.views/t [::recent "Recent"])]

            (for [recent (take 5 recent-documents)]
              ^{:key (:id recent)}
              [recent-document recent])])

         [:h2.mb-3.mt-8.text-2xl
          (i18n.views/t [::help "Help"])]

         (->> (conj [(action.views/deref-action :dialog/command-panel)]
                    (->> :app/help
                         (action.views/deref-action-group)
                         :actions))
              (flatten)
              (map command)
              (into [:div]))]]]]]]])
