(ns renderer.app.views
  (:require
   ["@radix-ui/react-direction" :as Direction]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-tooltip" :as Tooltip]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.action.views :as action.views]
   [renderer.app.events :as-alias app.events]
   [renderer.app.subs :as-alias app.subs]
   [renderer.dialog.views :as dialog.views]
   [renderer.document.subs :as-alias document.subs]
   [renderer.document.views :as document.views]
   [renderer.element.subs :as-alias element.subs]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.frame.views :as frame.views]
   [renderer.history.views :as history.views]
   [renderer.home.views :as home.views]
   [renderer.i18n.subs :as-alias i18n.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.menubar.views :as menubar.views]
   [renderer.panel.subs :as-alias panel.subs]
   [renderer.panel.views :as panel.views]
   [renderer.reepl.views :as repl.views]
   [renderer.ruler.views :as ruler.views]
   [renderer.snap.subs :as-alias snap.subs]
   [renderer.theme.subs :as-alias theme.subs]
   [renderer.timeline.views :as timeline.views]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.toolbar.status :as toolbar.status]
   [renderer.toolbar.tools :as toolbar.tools]
   [renderer.toolbar.views :as toolbar.views]
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
        active-pointers @(rf/subscribe [::app.subs/active-pointers])
        pinch-distance @(rf/subscribe [::app.subs/pinch-distance])
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
     ["Active pointers" (coll->str (keys active-pointers))]
     ["Pinch distance" (str pinch-distance)]
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

(defn debug-info
  []
  [:div
   {:dir "ltr"}
   (into [:div.absolute.top-2.left-2.pointer-events-none.text-gray-500]
         (for [[s v] (debug-rows)]
           [:div.flex
            [:strong.mr-1 s]
            [:div v]]))])

(defn read-only-overlay
  []
  [:div.absolute.inset-0.border-4.border-accent.pointer-events-none
   (when-let [preview-label @(rf/subscribe [::document.subs/preview-label])]
     [:div.absolute.bg-accent.top-2.left-2.px-1.rounded.text-accent-foreground
      preview-label])])

(defn right-panel
  [active-tool]
  [:div.flex.flex-col.h-full.bg-secondary.grow.overflow-hidden
   [views/scroll-area
    (tool.hierarchy/right-panel active-tool)]
   [:div.bg-primary.grow.flex]])

(defn guides-locked-toggle
  [class]
  (let [locked? @(rf/subscribe [::app.subs/guides-locked?])]
    [:div
     {:class class
      :style {:width ruler.views/ruler-size
              :height ruler.views/ruler-size}}
     [views/icon-button
      (if locked? "lock" "unlock")
      {:class "button-size-small rounded-xs m-0 bg-transparent!"
       :disabled @(rf/subscribe [::tool.subs/active? :guide])
       :title (i18n.views/t (if locked?
                              [::unlock "Unlock"]
                              [::lock "Lock"]))
       :on-click #(rf/dispatch [::app.events/toggle-guides-locked])}]]))

(defn frame
  []
  (let [backdrop @(rf/subscribe [::app.subs/backdrop])
        read-only? @(rf/subscribe [::document.subs/read-only?])
        help-message @(rf/subscribe [::tool.subs/help])
        help-bar @(rf/subscribe [::app.subs/help-bar])
        debug-info? @(rf/subscribe [::app.subs/debug-info])
        worker-active? @(rf/subscribe [::worker.subs/some-active?])
        xl? @(rf/subscribe [::window.subs/xl?])]
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
        {:on-click #(rf/dispatch [::app.events/set-backdrop false])}])]))

(defn context-dropdown-button
  []
  [:> DropdownMenu/Root
   [:> DropdownMenu/Trigger
    {:as-child true}
    [:button.button.flex.items-center.justify-center.px-2.font-mono.rounded
     {:aria-label (i18n.views/t [::more-actions "More object actions"])}
     [views/icon "ellipsis-h"]]]
   [:> DropdownMenu/Portal
    (->> frame.views/context-menu-action-groups
         (map (comp :actions action.views/deref-action-group))
         (interpose {:type :separator})
         (flatten)
         (map views/dropdown-menu-item)
         (into [:> DropdownMenu/Content
                {:side "left"
                 :align "end"
                 :class "menu-content rounded-sm"
                 :on-key-down #(.stopPropagation %)
                 :on-escape-key-down #(.stopPropagation %)}
                [views/dropdownmenu-arrow]]))]])

(defn frame-panel
  []
  (let [rulers? @(rf/subscribe [::app.subs/rulers?])
        md? @(rf/subscribe [::window.subs/md?])
        active? @(rf/subscribe [::tool.subs/active? :guide])
        bg-class (if active? "bg-accent" "bg-primary")]
    [:div.flex.flex-col.flex-1.h-full.gap-px.overflow-hidden
     [:div
      [toolbar.tools/root]
      (when rulers?
        [:div.flex.gap-px
         [:div
          {:style {:width ruler.views/ruler-size
                   :height ruler.views/ruler-size}}
          [guides-locked-toggle bg-class]]
         [:div.flex-1
          {:dir "ltr"
           :class ["rtl:pl-[50px] rtl:md:pl-0" bg-class]}
          [ruler.views/ruler :horizontal]]])]
     [:div.flex.flex-1.relative.gap-px
      (when rulers?
        [:div
         {:dir "ltr"
          :class ["rtl:scale-x-[-1]" bg-class]}
         [ruler.views/ruler :vertical]])
      [:div.relative.grow.flex
       [frame]
       (when-not md?
         [:div.bg-primary.flex.items-center
          [toolbar.views/action-toolbar
           {:orientation :vertical
            :actions [:object/index-operations
                      :object/horizontal-alignment
                      :object/vertical-alignment]}
           [:span.h-divider]
           [context-dropdown-button]]])]]]))

(defn xml-panel
  []
  (let [xml @(rf/subscribe [::element.subs/xml])
        codemirror-theme @(rf/subscribe [::theme.subs/codemirror])]
    [views/scroll-area
     [:div.p-1
      [views/cm-editor xml
       {:options {:mode "text/xml"
                  :readOnly true
                  :screenReaderLabel "XML"
                  :theme codemirror-theme}}]]]))

(defn center-top-group
  []
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
        :minSize 320}
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

(defn editor
  []
  (let [timeline-visible @(rf/subscribe [::panel.subs/visible? :timeline])
        md? @(rf/subscribe [::window.subs/md?])]
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
     [panel.views/separator]
     [toolbar.status/root]
     (when md? [repl.views/root])]))

(defn bottom-bar
  []
  (let [some-selected? @(rf/subscribe [::element.subs/some-selected?])
        active-tool @(rf/subscribe [::tool.subs/active])
        mac? @(rf/subscribe [::app.subs/mac?])]
    [:div.flex.justify-evenly.p-2.gap-1.rtl:flex-row-reverse

     [views/drawer
      {:icon "tree"
       :label [::tree "Tree"]
       :direction "left"
       :content-class (when mac? "pt-8")}
      [tree.views/root]]

     [views/drawer
      {:icon "code"
       :label [::xml "XML"]
       :direction "left"
       :content-class (when mac? "pt-8")}
      [xml-panel]]

     [:span.v-divider]

     [views/drawer
      {:icon "animation"
       :label [::timeline "Timeline"]
       :direction "bottom"}
      [timeline.views/root]]

     [views/drawer
      {:icon "shell"
       :label [::shell "Shell"]
       :direction "bottom"}
      [:div.flex.flex-col.flex-1
       [repl.views/root]]]

     [:span.v-divider]

     [views/drawer
      {:icon "history"
       :label [::history "History"]
       :direction "right"}
      [history.views/root]]

     [views/drawer
      {:icon "properties"
       :label [::attributes "Attributes"]
       :direction "right"
       :disabled (not some-selected?)}
      [right-panel active-tool]]]))

(defn center-panel
  []
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
         :minSize 320}
        [:div.flex.h-full.flex-col.flex-1.overflow-hidden.gap-px.w-full
         [editor]]]
       (when (and md? properties?)
         [:<>
          [panel.views/separator]
          [panel.views/panel
           {:id :properties
            :defaultSize 320
            :minSize 320
            :groupResizeBehavior "preserve-pixel-size"
            :class "flex gap-px"}
           [right-panel active-tool]]])]
      (when md?
        [:div.bg-primary.flex
         [toolbar.views/action-toolbar
          {:orientation :vertical
           :actions [:object/index-operations
                     :object/horizontal-alignment
                     :object/vertical-alignment
                     :object/boolean-operations]}]])]]))

(defn main-panel-group
  []
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
            :minSize 227
            :groupResizeBehavior "preserve-pixel-size"}
           [:div.flex.flex-col.overflow-hidden.h-full
            [document.views/actions]
            [tree.views/root]]]
          [panel.views/separator]])
       [panel.views/panel
        {:defaultSize "100%"
         :minSize 665}
        [center-panel]]]]
     (when-not md?
       [bottom-bar])]))

(defn root
  []
  (let [documents? @(rf/subscribe [::document.subs/some-entities?])
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
           [home.views/root recent-documents])
         [:div]]
        [dialog.views/root]
        [views/toaster theme]]])))
