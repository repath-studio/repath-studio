(ns renderer.app.views
  (:require
   ["@radix-ui/react-direction" :as Direction]
   ["@radix-ui/react-select" :as Select]
   ["@radix-ui/react-tooltip" :as Tooltip]
   ["path-browserify" :as path-browserify]
   [clojure.string :as string]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.app.subs :as-alias app.subs]
   [renderer.db :as db]
   [renderer.dialog.events :as-alias dialog.events]
   [renderer.dialog.views :as dialog.views]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.document.views :as document.views]
   [renderer.element.subs :as-alias element.subs]
   [renderer.events :as-alias events]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.frame.views :as frame.views]
   [renderer.history.views :as history.views]
   [renderer.i18n.subs :as-alias i18n.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.menubar.views :as menubar.views]
   [renderer.panel.subs :as-alias panel.subs]
   [renderer.panel.views :as panel.views]
   [renderer.reepl.views :as repl.views]
   [renderer.ruler.events :as-alias ruler.events]
   [renderer.ruler.subs :as-alias ruler.subs]
   [renderer.ruler.views :as ruler.views]
   [renderer.snap.subs :as-alias snap.subs]
   [renderer.theme.subs :as-alias theme.subs]
   [renderer.timeline.views :as timeline.views]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.toolbar.object :as toolbar.object]
   [renderer.toolbar.status :as toolbar.status]
   [renderer.toolbar.tools :as toolbar.tools]
   [renderer.tree.views :as tree.views]
   [renderer.utils.length :as utils.length]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]
   [renderer.window.views :as window.views]
   [renderer.worker.subs :as-alias worker.subs]))

(defn coll->str
  [coll]
  (str "[" (string/join " " (map utils.length/->fixed coll)) "]"))

(defn map->str
  [m]
  (->> m
       (map (fn [[k v]]
              ^{:key k}
              [:span (str (name k) ": " (if (number? v)
                                          (utils.length/->fixed v)
                                          (coll->str v)))]))
       (interpose ", ")))

(defn debug-rows
  []
  (let [viewbox @(rf/subscribe [::frame.subs/viewbox])
        pointer-pos @(rf/subscribe [::app.subs/pointer-pos])
        adjusted-pos @(rf/subscribe [::app.subs/adjusted-pointer-pos])
        pointer-offset @(rf/subscribe [::app.subs/pointer-offset])
        adjusted-offset @(rf/subscribe [::app.subs/adjusted-pointer-offset])
        drag? @(rf/subscribe [::tool.subs/drag?])
        pan @(rf/subscribe [::document.subs/pan])
        active-tool @(rf/subscribe [::tool.subs/active])
        cached-tool @(rf/subscribe [::tool.subs/cached])
        tool-state @(rf/subscribe [::tool.subs/state])
        clicked-element @(rf/subscribe [::app.subs/clicked-element])
        ignored-ids @(rf/subscribe [::document.subs/ignored-ids])
        nearest-neighbor @(rf/subscribe [::snap.subs/nearest-neighbor])]
    [["Viewbox" (coll->str viewbox)]
     ["Pointer position" (coll->str pointer-pos)]
     ["Adjusted pointer position" (coll->str adjusted-pos)]
     ["Pointer offset" (coll->str pointer-offset)]
     ["Adjusted pointer offset" (coll->str adjusted-offset)]
     ["Pointer drag?" (str drag?)]
     ["Pan" (coll->str pan)]
     ["Active tool" active-tool]
     ["Cached tool" cached-tool]
     ["State" tool-state]
     ["Clicked element" (:id clicked-element)]
     ["Ignored elements" ignored-ids]
     ["Snap" (map->str nearest-neighbor)]]))

(defn debug-info []
  [:div
   {:dir "ltr"}
   (into [:div.absolute.top-2.left-2.pointer-events-none.text-gray-500]
         (for [[s v] (debug-rows)]
           [:div.flex
            [:strong.mr-1 s]
            [:div v]]))])

(defn read-only-overlay []
  [:div.absolute.inset-0.border-4.border-accent.pointer-events-none
   (when-let [preview-label @(rf/subscribe [::document.subs/preview-label])]
     [:div.absolute.bg-accent.top-2.left-2.px-1.rounded.text-accent-foreground
      preview-label])])

(defn right-panel
  [active-tool]
  [:div.flex.flex-col.h-full.bg-secondary.grow
   [views/scroll-area
    (tool.hierarchy/right-panel active-tool)]
   [:div.bg-primary.grow.flex]])

(defn ruler-locked-toggle
  []
  (let [ruler-locked? @(rf/subscribe [::ruler.subs/locked?])]
    [:div.bg-primary
     {:style {:width ruler.views/ruler-size
              :height ruler.views/ruler-size}}
     [views/icon-button
      (if ruler-locked? "lock" "unlock")
      {:class "button-size-small rounded-xs m-0 bg-transparent! hidden"
       :title (i18n.views/t (if ruler-locked?
                              [::unlock "Unlock"]
                              [::lock "Lock"]))
       :on-click #(rf/dispatch [::ruler.events/toggle-locked])}]]))

(defn frame-panel []
  (let [ruler-visible? @(rf/subscribe [::ruler.subs/visible?])
        backdrop @(rf/subscribe [::app.subs/backdrop])
        read-only? @(rf/subscribe [::document.subs/read-only?])
        help-message @(rf/subscribe [::tool.subs/help])
        help-bar @(rf/subscribe [::app.subs/help-bar])
        debug-info? @(rf/subscribe [::app.subs/debug-info])
        worker-active? @(rf/subscribe [::worker.subs/some-active?])
        md? @(rf/subscribe [::window.subs/md?])
        xl? @(rf/subscribe [::window.subs/xl?])]
    [:div.flex.flex-col.flex-1.h-full.gap-px
     [:div
      [toolbar.tools/root]
      (when ruler-visible?
        [:div.flex.gap-px
         [:div.bg-primary
          {:style {:width ruler.views/ruler-size
                   :height ruler.views/ruler-size}}
          [ruler-locked-toggle]]
         [:div.bg-primary.flex-1
          {:dir "ltr"
           :class "rtl:pl-[50px] rtl:md:pl-0"}
          [ruler.views/ruler :horizontal]]])]
     [:div.flex.flex-1.relative.gap-px
      (when ruler-visible?
        [:div.bg-primary
         {:dir "ltr"
          :class "rtl:scale-x-[-1]"}
         [ruler.views/ruler :vertical]])
      [:div.relative.grow.flex
       [:div.grow.flex.relative
        {:data-theme "light"
         :style {:background "var(--secondary)"}}
        [frame.views/root]
        [:div.absolute.inset-0.pointer-events-none.inset-shadow]
        (when read-only? [read-only-overlay])
        (when debug-info? [debug-info])
        (when worker-active?
          [:div.absolute.bottom-2.right-2.text-gray-500
           [views/loading-indicator]])
        (when (and help-bar (seq help-message) xl?)
          [views/help help-message])
        (when backdrop
          [:div.absolute.inset-0
           {:on-click #(rf/dispatch [::app.events/set-backdrop false])}])]
       (when-not md?
         [:div.bg-primary.flex.items-center
          [toolbar.object/root]])]]]))

(defn xml-panel []
  (let [xml @(rf/subscribe [::element.subs/xml])
        codemirror-theme @(rf/subscribe [::theme.subs/codemirror])]
    [views/scroll-area
     [:div.p-1
      [views/cm-editor xml
       {:options {:mode "text/xml"
                  :readOnly true
                  :screenReaderLabel "XML"
                  :theme codemirror-theme}}]]]))

(defn center-top-group []
  (let [md? @(rf/subscribe [::window.subs/md?])
        history-visible? @(rf/subscribe [::panel.subs/visible? :history])
        xml-visible? @(rf/subscribe [::panel.subs/visible? :xml])]
    [:div.flex.flex-col.flex-1.h-full
     [panel.views/group
      {:orientation "horizontal"
       :id "center-top-group"
       :class "h-full"}
      (when (and md? xml-visible?)
        [:<>
         [panel.views/panel
          {:id :xml
           :class "relative"
           :defaultSize 300
           :minSize 100}
          [:div.h-full.bg-primary.flex
           [xml-panel]]
          [panel.views/close-button :xml]]
         [panel.views/separator]])

      [panel.views/panel
       {:defaultSize "100%"
        :minSize 100}
       [frame-panel]]

      (when (and md? history-visible?)
        [:<>
         [panel.views/separator]
         [panel.views/panel
          {:id :history
           :class "relative"
           :defaultSize 300
           :minSize 100}
          [:div.bg-primary.h-full
           [history.views/root]]
          [panel.views/close-button :history]]])]]))

(defn editor []
  (let [timeline-visible @(rf/subscribe [::panel.subs/visible? :timeline])
        md? @(rf/subscribe [::window.subs/md?])
        repl-history? @(rf/subscribe [::panel.subs/visible? :repl-history])]
    [panel.views/group
     {:orientation "vertical"
      :id "editor-group"
      :class "h-full"}
     [panel.views/panel
      {:defaultSize "100%"
       :minSize 100}
      [center-top-group]]

     (when (and md? timeline-visible)
       [:<>
        [panel.views/separator]
        [panel.views/panel
         {:id :timeline
          :class "relative"
          :minSize 100
          :defaultSize 300}
         [timeline.views/root]
         [panel.views/close-button :timeline]]])
     (when (and md? repl-history?) [panel.views/separator])
     [toolbar.status/root]
     (when md? [repl.views/root])]))

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

(defn home
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

(defn bottom-bar []
  (let [some-selected? @(rf/subscribe [::element.subs/some-selected?])
        active-tool @(rf/subscribe [::tool.subs/active])]
    [:div.flex.justify-evenly.p-2.gap-1.rtl:flex-row-reverse

     [views/drawer
      {:icon "tree"
       :label [::tree "Tree"]
       :direction "left"
       :content [tree.views/root]}]

     [views/drawer
      {:icon "code"
       :label [::xml "XML"]
       :direction "left"
       :content [xml-panel]}]
     [:span.v-divider]

     [views/drawer
      {:icon "animation"
       :label [::timeline "Timeline"]
       :direction "bottom"
       :content [timeline.views/root]}]

     [views/drawer
      {:icon "shell"
       :label [::shell "Shell"]
       :direction "bottom"
       :content [:div.flex.flex-col.flex-1 [repl.views/root]]}]

     [:span.v-divider]

     [views/drawer
      {:icon "history"
       :label [::history "History"]
       :direction "right"
       :content [history.views/root]}]

     [views/drawer
      {:icon "properties"
       :label [::attributes "Attributes"]
       :direction "right"
       :disabled (not some-selected?)
       :content [right-panel active-tool]}]]))

(defn center-panel []
  (let [properties? @(rf/subscribe [::panel.subs/visible? :properties])
        active-tool @(rf/subscribe [::tool.subs/active])
        md? @(rf/subscribe [::window.subs/md?])
        desktop? @(rf/subscribe [::app.subs/desktop?])]
    [:div.flex.flex-col.flex-1.overflow-hidden.h-full
     (if md?
       [document.views/tab-bar]
       [:div.flex.overflow-hidden
        (when-not desktop?
          [views/toolbar [menubar.views/root]])
        [document.views/tab-bar]
        [:div.drag.flex-1]])
     [:div.flex.h-full.flex-1.gap-px.overflow-hidden
      [panel.views/group
       {:orientation "horizontal"
        :id "main-editor-group"
        :class "w-full"}
       [panel.views/panel
        {:defaultSize "100%"
         :minSize 100}
        [:div.flex.h-full.flex-col.flex-1.overflow-hidden.gap-px.w-full
         [editor]]]
       (when (and md? properties?)
         [:<>
          [panel.views/separator]
          [panel.views/panel
           {:id :properties
            :defaultSize 320
            :minSize 320
            :class "flex gap-px"}
           (when properties?
             [right-panel active-tool])]])]
      (when md?
        [:div.bg-primary.flex
         [toolbar.object/root]])]]))

(defn main-panel-group []
  (let [tree? @(rf/subscribe [::panel.subs/visible? :tree])
        md? @(rf/subscribe [::window.subs/md?])]
    [:div.flex.flex-col.h-full.overflow-hidden
     [:div.flex.flex-1.overflow-hidden.gap-px
      [panel.views/group
       {:orientation "horizontal"
        :id "main-group"
        :class "w-full"}
       (when (and tree? md?)
         [:<>
          [panel.views/panel
           {:id :tree
            :defaultSize 227
            :minSize 227}
           [:div.flex.flex-col.overflow-hidden.h-full
            [document.views/actions]
            [tree.views/root]]]
          [panel.views/separator]])
       [panel.views/panel
        {:defaultSize "100%"
         :minSize 100}
        [center-panel]]]]
     (when-not md?
       [bottom-bar])]))

(defn root []
  (let [documents? @(rf/subscribe [::document.subs/entities?])
        recent-documents @(rf/subscribe [::document.subs/recent])
        lang-dir @(rf/subscribe [::i18n.subs/lang-dir])
        desktop? @(rf/subscribe [::app.subs/desktop?])
        md? @(rf/subscribe [::window.subs/md?])
        loading? @(rf/subscribe [::app.subs/loading?])
        theme @(rf/subscribe [::theme.subs/theme])]
    (if loading?
      (when-not desktop? [:div.loader])
      [:> Direction/Provider {:dir lang-dir}
       [:> Tooltip/Provider
        [:div.flex.flex-col.h-full.overflow-hidden.justify-between
         (if (or md? desktop?)
           [window.views/app-header]
           [:div])
         (if documents?
           [main-panel-group]
           [home recent-documents])
         [:div]]
        [dialog.views/root]
        [views/toaster theme]]])))
