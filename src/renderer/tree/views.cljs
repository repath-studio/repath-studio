(ns renderer.tree.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.events :as-alias events]
   [renderer.frame.events :as-alias frame.events]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.tree.effects :as tree.effects]
   [renderer.tree.events :as-alias tree.events]
   [renderer.utils.dom :as utils.dom]
   [renderer.utils.element :as utils.element]
   [renderer.utils.key :as utils.key]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(defn item-prop-toggle
  [id state k active-icon inactive-icon active-title inactive-title]
  (let [title (if state active-title inactive-title)]
    [views/icon-button
     (if state
       active-icon
       inactive-icon)
     {:class ["text-inherit! bg-transparent! outline-inset!
               group-hover:opacity-100 focus:opacity-100 outline-inherit
               button-size-small rounded-xs m-0"
              (when (not state) "opacity-30 md:opacity-0")]
      :title (i18n.views/t title)
      :on-double-click #(.stopPropagation %)
      :on-click (fn [e]
                  (.stopPropagation e)
                  (rf/dispatch [::element.events/toggle-prop id k title]))}]))

(defn set-item-label!
  [e id]
  (rf/dispatch-sync [::element.events/set-label id (.. e -target -value)])
  (when-not (.-relatedTarget e)
    (.focus (tree.effects/query-by-id! id))))

(defn item-label
  [el]
  (let [{:keys [id label visible selected tag]} el
        properties (element.hierarchy/properties tag)
        tag-label (or (some-> properties :label i18n.views/t)
                      (string/capitalize (name tag)))]
    (reagent/with-let [edit-mode? (reagent/atom false)]
      (if @edit-mode?
        [:input.bg-transparent.w-full
         {:class ["font-[inherit]! leading-[inherit]!"
                  (when (= :svg tag) "font-bold")]
          :default-value label
          :placeholder tag-label
          :auto-focus true
          :draggable true ; Prevents drag of parent.
          :enter-key-hint "done"
          :on-drag-start #(.preventDefault %)
          :on-focus #(.. % -target select)
          :on-key-down #(utils.key/down-handler! % label set-item-label! id)
          :on-blur (fn [e]
                     (reset! edit-mode? false)
                     (set-item-label! e id))}]
        [:div.flex.w-full.overflow-hidden
         [:div.truncate
          {:class [(when-not visible "opacity-60")
                   (when (= :svg tag) "font-bold")]
           :style {:cursor (when selected "text")}
           :on-double-click (fn [e]
                              (.stopPropagation e)
                              (reset! edit-mode? true))}
          (if (empty? label)
            tag-label
            label)]]))))

(defn drop-handler!
  [e parent-id]
  (let [id (utils.dom/event->uuid e)]
    (.preventDefault e)
    (rf/dispatch [::element.events/set-parent id parent-id])))

(def last-focused-id (reagent/atom nil))

(defn set-last-focused-id!
  [id]
  (reset! last-focused-id id))

(defn key-down-handler!
  [e id]
  (case (.-code e)
    "ArrowUp"
    (do (.stopPropagation e)
        (rf/dispatch [::tree.events/focus-up id]))

    "ArrowDown"
    (do (.stopPropagation e)
        (rf/dispatch [::tree.events/focus-down id]))

    "ArrowLeft"
    (do (.stopPropagation e)
        (rf/dispatch [::document.events/collapse-el id]))

    "ArrowRight"
    (do (.stopPropagation e)
        (rf/dispatch [::document.events/expand-el id]))

    "Enter"
    (do (.stopPropagation e)
        (rf/dispatch [::element.events/select id (.-ctrlKey e)]))

    "Space"
    (do (.stopPropagation e)
        (rf/dispatch [::element.events/select id (.-ctrlKey e)]))

    nil))

(defn collapse-button
  [id collapsed]
  [views/icon-button
   (if collapsed "chevron-right" "chevron-down")
   {:title (if collapsed "expand" "collapse")
    :class "bg-transparent! text-inherit! rtl:scale-x-[-1] outline-inset! m-0
            outline-inherit button-size-small rounded-xs"
    :on-double-click #(.stopPropagation %)
    :on-click #(do (.stopPropagation %)
                   (rf/dispatch (if collapsed
                                  [::document.events/expand-el id]
                                  [::document.events/collapse-el id])))}])

(defn list-item-button
  [el {:keys [depth collapsed hovered]}]
  (let [{:keys [id selected children locked visible]} el
        md? @(rf/subscribe [::window.subs/md?])
        collapse-button-width (if md? 21 27) ; TODO: Get from CSS variable.
        padding (* collapse-button-width (cond-> depth (seq children) dec))]
    [:div.list-item-button.button.flex.px-1.items-center.text-start.group
     {:class ["hover:bg-overlay outline-inset"
              (when selected "accent")
              (when hovered "bg-overlay")
              (when-not md? "h-10")]
      :tab-index 0
      :data-id (str id)
      :on-double-click #(rf/dispatch [::frame.events/pan-to-element id])
      :on-pointer-enter #(rf/dispatch [::document.events/set-hovered-id id])
      :ref (fn [this]
             (when (and this selected)
               (rf/dispatch [::events/scroll-into-view this])
               (set-last-focused-id! (.getAttribute this "data-id"))))
      :draggable true
      :on-key-down #(key-down-handler! % id)
      :on-drag-start #(-> % .-dataTransfer (.setData "id" (str id)))
      :on-drag-enter #(rf/dispatch [::document.events/set-hovered-id id])
      :on-drag-over #(.preventDefault %)
      :on-drop #(drop-handler! % id)
      :on-pointer-down #(when (= (.-button %) 2)
                          (rf/dispatch [::element.events/select
                                        id (.-ctrlKey %)]))
      :on-click (fn [e]
                  (.stopPropagation e)
                  (if (.-shiftKey e)
                    (rf/dispatch-sync [::tree.events/select-range
                                       @last-focused-id id])
                    (do (rf/dispatch [::element.events/select id (.-ctrlKey e)])
                        (reset! last-focused-id id))))}
     [:div.shrink-0 {:style {:flex-basis padding}}]
     [:div.flex-1.flex.items-center.justify-between.w-full.overflow-hidden
      (when (seq children)
        [collapse-button id collapsed])
      [:div.flex-1.overflow-hidden.flex.items-center
       {:class "gap-1.5"}
       (when-let [icon (:icon (utils.element/properties el))]
         [views/icon icon {:class (when-not visible "opacity-60")}])
       [item-label el]]
      [item-prop-toggle id
       locked :locked
       "lock" "unlock"
       [::unlock "Unlock"] [::lock "Lock"]]
      [item-prop-toggle id
       (not visible) :visible
       "eye-closed" "eye"
       [::show "Show"] [::hide "Hide"]]]]))

(defn item [el depth elements]
  (let [{:keys [selected children id]} el
        has-children (seq children)
        hovered-ids @(rf/subscribe [::document.subs/hovered-ids])
        collapsed-ids @(rf/subscribe [::document.subs/collapsed-ids])
        collapsed (contains? collapsed-ids id)]
    [:li {:class (when selected "bg-overlay")
          :role "menuitem"}
     [list-item-button el {:depth depth
                           :collapsed collapsed
                           :hovered (contains? hovered-ids id)}]
     (when (and has-children (not collapsed))
       [:ul {:role "menu"}
        (for [el (mapv (fn [k] (get elements k)) (reverse children))]
          ^{:key (:id el)}
          [item el (inc depth) elements])])]))

(defn inner-sidebar-render
  [root-children elements]
  (let [md? @(rf/subscribe [::window.subs/md?])]
    [:div#tree-sidebar.flex.flex-1.bg-primary.h-full.overflow-hidden
     ;; When the tree is hovered, ignore the hovered class of the elements,
     ;; if the element itself is not also hovered.
     {:class "hover:**:[&.list-item-button]:not-hover:bg-inherit"
      :on-click #(rf/dispatch [::element.events/deselect-all])}
     [views/scroll-area
      [:ul.overflow-hidden
       {:role "menu"
        :on-pointer-leave #(rf/dispatch [::document.events/clear-hovered])
        :class (if md? "w-[227px]" "w-full")}
       (for [el (reverse root-children)]
         ^{:key (:id el)}
         [item el 1 elements])]]]))

(defn inner-sidebar []
  (let [state @(rf/subscribe [::tool.subs/state])
        root-children @(rf/subscribe [::element.subs/root-children])
        elements @(rf/subscribe [::document.subs/elements])]
    (if (= state :idle)
      [inner-sidebar-render root-children elements]
      (reagent/with-let [root-children root-children
                         elements elements]
        [inner-sidebar-render root-children elements]))))

(defn root
  []
  [:> ContextMenu/Root
   [:> ContextMenu/Trigger
    {:class "flex h-full w-full overflow-hidden"}
    [inner-sidebar]]
   [:> ContextMenu/Portal
    (into [:> ContextMenu/Content
           {:class "menu-content context-menu-content"
            :on-escape-key-down #(.stopPropagation %)}]
          (map (fn [menu-item] [views/context-menu-item menu-item])
               (element.views/context-menu)))]])
