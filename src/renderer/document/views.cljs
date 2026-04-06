(ns renderer.document.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.action.views :as action.views]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.events :as-alias events]
   [renderer.history.subs :as-alias history.subs]
   [renderer.history.views :as history.views]
   [renderer.i18n.views :as i18n.views]
   [renderer.panel.subs :as-alias panel.subs]
   [renderer.utils.dom :as utils.dom]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(defn actions []
  (let [undos @(rf/subscribe [::history.subs/undos])
        redos @(rf/subscribe [::history.subs/redos])
        md? @(rf/subscribe [::window.subs/md?])
        new-action (action.views/deref-action :document/new)
        open-action (action.views/deref-action :document/open)
        save-action (action.views/deref-action :document/save)
        download-action (action.views/deref-action :document/download)
        undo-action (action.views/deref-action :history/undo)
        redo-action (action.views/deref-action :history/redo)]
    [views/toolbar

     (->> [new-action
           open-action
           (or save-action download-action)]
          (map #(views/action-icon-button % :title (action.views/label %)))
          (into [:<>]))

     [:span.v-divider]

     [history.views/action-button
      (merge undo-action {:options undos
                          :options-label [::undo-stack "Undo stack"]
                          :show-options md?})]

     [history.views/action-button
      (merge redo-action {:options redos
                          :options-label [::redo-stack "Redo stack"]
                          :show-options md?})]]))

(defn close-button
  [id saved]
  [:button.button.button-size-small.invisible.relative.shrink-0.bg-inherit.group
   {:key id
    :class "hover:[&_.dot-icon]:hidden focus:[&_.dot-icon]:hidden rounded-xs
            flex items-center justify-center"
    :title (i18n.views/t [::close-doc "Close document"])
    :on-click (fn [e]
                (.stopPropagation e)
                (rf/dispatch [::document.events/close id true]))}
   [views/icon "times"]
   (when-not saved
     [views/icon "dot"
      {:class "absolute top-0.5 left-0.5 bg-inherit items-center
               text-foreground-muted invisible md:visible group-hover:invisible
               group-focus:invisible group-active:invisible"}])])

(defn context-menu
  [id]
  (let [document @(rf/subscribe [::document.subs/entity id])
        path (:path document)
        tabs @(rf/subscribe [::document.subs/tabs])
        desktop? @(rf/subscribe [::app.subs/desktop?])]
    (cond-> [{:label [::close "Close"]
              :event [::document.events/close id true]}
             {:label [::close-others "Close others"]
              :event [::document.events/close-others id]
              :enabled (boolean (seq (rest tabs)))}
             (action.views/deref-action :document/close-saved)
             (action.views/deref-action :document/close-all)]
      desktop?
      (concat [{:type :separator}
               {:label [::open-directory "Open containing directory"]
                :event [::document.events/open-directory path]
                :enabled (boolean (seq path))}]))))

(defn on-tab-drop
  [id dragged-over? e]
  (let [dropped-id (utils.dom/event->uuid e)]
    (.preventDefault e)
    (reset! dragged-over? false)
    (rf/dispatch [::document.events/swap-position dropped-id id])))

(defn document-title
  [id]
  (let [document @(rf/subscribe [::document.subs/entity id])
        saved? @(rf/subscribe [::document.subs/saved? id])
        {:keys [title]} document]
    [:div.pointer-events-none.px-2.gap-1.flex.overflow-hidden
     (when-not saved?
       [:span.md:hidden "•"])
     [:span.truncate title]]))

(defn tab-button-classes
  [active? saved?]
  ["flex items-center h-full relative text-left px-2 py-0 overflow-hidden
    hover:[&_button]:visible outline-default hover:text-foreground
    outline-inset"
   (if active?
     "bg-primary text-foreground [&_button]:visible"
     "bg-secondary text-foreground-muted")
   (when-not saved?
     "[&_button]:visible")])

(defn tab-button
  [id]
  (reagent/with-let [dragged-over? (reagent/atom false)]
    (let [saved? @(rf/subscribe [::document.subs/saved? id])
          active? @(rf/subscribe [::document.subs/active? id])]
      [:div.tab
       {:class (tab-button-classes active? saved?)
        :on-wheel #(when-not (zero? (.-deltaY %))
                     (rf/dispatch [::document.events/cycle (.-deltaY %)]))
        :on-click #(rf/dispatch [::document.events/set-active id])
        :on-pointer-up #(when (= (.-button %) 1)
                          (rf/dispatch [::document.events/close id true]))
        :draggable true
        :tab-index 0
        :on-key-down #(when (= (.-key %) "Enter")
                        (rf/dispatch [::document.events/set-active id]))
        :on-drag-start #(.setData (.-dataTransfer %) "id" (str id))
        :on-drag-over #(.preventDefault %)
        :on-drag-enter #(reset! dragged-over? true)
        :on-drag-leave #(reset! dragged-over? false)
        :on-drop (partial on-tab-drop id dragged-over?)
        :ref #(when (and % active?)
                (rf/dispatch [::events/scroll-into-view %]))}
       [document-title id]
       [close-button id saved?]])))

(defn tab
  [id]
  [:> ContextMenu/Root
   [:> ContextMenu/Trigger
    {:as-child true}
    [:div.overflow-hidden.flex [tab-button id]]]
   [:> ContextMenu/Portal
    (->> (context-menu id)
         (map views/context-menu-item)
         (into [:> ContextMenu/Content
                {:class "menu-content context-menu-content"
                 :on-escape-key-down #(.stopPropagation %)}]))]])

(defn documents-dropdown-button []
  (let [documents @(rf/subscribe [::document.subs/entities])
        md? @(rf/subscribe [::window.subs/md?])
        document-count (count documents)]
    [:> DropdownMenu/Root
     [:> DropdownMenu/Trigger
      {:as-child true}
      [:button.button.flex.items-center.justify-center.px-2.font-mono.rounded
       {:title (i18n.views/t [::more-actions "More document actions"])}
       [:div.flex.gap-1.items-center
        (when-not (or md? (= document-count 1))
          document-count)
        [views/icon (if md? "ellipsis-h" "chevron-down")]]]]
     [:> DropdownMenu/Portal
      (cond->> [:document/close-saved
                :document/close-all]

        :always
        (map (comp #(dissoc % :icon)
                   action.views/deref-action))

        (and (seq documents)
             (not md?))
        (concat (mapv (fn [{:keys [id title]}]
                        {:key id
                         :label [(keyword id) title]
                         :event [::document.events/set-active id]
                         :active [::document.subs/active? id]})
                      documents)
                [{:type :separator}])

        :always
        (into [:> DropdownMenu/Content
               {:side "bottom"
                :align "start"
                :class "menu-content rounded-sm"
                :on-key-down #(.stopPropagation %)
                :on-escape-key-down #(.stopPropagation %)}
               [views/dropdownmenu-arrow]]
              (map views/dropdown-menu-item)))]]))

(defn mobile-tabs []
  (let [documents @(rf/subscribe [::document.subs/entities])
        active-id @(rf/subscribe [::document.subs/active-id])]
    [:div.flex.overflow-hidden.gap-px
     (when (second documents)
       [views/toolbar
        {:class "bg-primary"}
        [documents-dropdown-button]])
     [tab active-id]]))

(defn tab-bar []
  (let [tabs @(rf/subscribe [::document.subs/tabs])
        md? @(rf/subscribe [::window.subs/md?])
        tree-visible @(rf/subscribe [::panel.subs/visible? :tree])]
    [:div.flex.justify-between.gap-px.overflow-hidden
     [:div.flex.flex-1.overflow-hidden.gap-px
      (when (and md? (not tree-visible))
        [actions])
      (if md?
        (for [document-id tabs]
          ^{:key document-id}
          [tab document-id])
        [mobile-tabs])
      (when-not md?
        [actions])
      [:div.drag.flex-1]]

     (when md?
       [views/toolbar [documents-dropdown-button]])]))
