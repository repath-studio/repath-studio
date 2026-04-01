(ns renderer.menubar.views
  (:require
   ["@radix-ui/react-menubar" :as Menubar]
   [re-frame.core :as rf]
   [renderer.a11y.events :as-alias a11y.events]
   [renderer.a11y.subs :as-alias a11y.subs]
   [renderer.action.views :as action.views]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.subs :as-alias element.subs]
   [renderer.i18n.events :as-alias i18n.events]
   [renderer.i18n.subs :as-alias i18n.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.menubar.events :as-alias menubar.events]
   [renderer.menubar.subs :as-alias menubar.subs]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(defn recent-submenu
  []
  (let [recent-documents @(rf/subscribe [::document.subs/recent])
        recent-items (->> recent-documents
                          (mapv (fn [{:keys [path title id]
                                      :as recent}]
                                  {:id id
                                   :label [(keyword id) (or path title)]
                                   :icon "folder"
                                   :event [::document.events/open-recent
                                           recent]})))]
    (cond-> recent-items
      (seq recent-items)
      (concat [:separator
               {:id :clear-recent
                :label [::recent-clear "Clear recent"]
                :icon "delete"
                :event [::document.events/clear-recent]}]))))

(def export-submenu
  [:export/svg
   :separator
   :export/png
   :export/jpg
   :export/webp
   :export/gif])

(defn file-menu []
  {:id :file
   :label [::file "File"]
   :type :root
   :items [:document/new
           :separator
           :document/open
           {:id :recent
            :label [::recent "Recent"]
            :type :sub-menu
            :enabled [::document.subs/recent?]
            :available [::app.subs/supported-feature? :file-system]
            :items (recent-submenu)}
           :separator
           :document/save
           :document/save-as
           :document/download
           {:id :export
            :label [::export-as "Export as"]
            :type :sub-menu
            :enabled [::document.subs/entities?]
            :items export-submenu}
           :separator
           :document/print
           :separator
           :document/close
           :window/close]})

(defn edit-menu []
  {:id :edit
   :label [::edit "Edit"]
   :type :root
   :enabled [::document.subs/entities?]
   :items [:history/undo
           :history/redo
           :separator
           :clipboard/cut
           :clipboard/copy
           :clipboard/paste
           :clipboard/paste-in-place
           :clipboard/paste-styles
           :separator
           :element/duplicate
           :separator
           :element/select-all
           :element/deselect-all
           :element/invert-selection
           :element/select-same-tags
           :separator
           :element/delete]})

(def align-submenu
  [:align/left
   :align/center-horizontal
   :align/right
   :separator
   :align/top
   :align/center-vertical
   :align/bottom])

(def boolean-submenu
  [:boolean/exclude
   :boolean/unite
   :boolean/intersect
   :boolean/subtract
   :boolean/divide])

(def animate-submenu
  [:animate/animate
   :animate/transform
   :animate/motion])

(def path-submenu
  [:path/simplify
   :path/smooth
   :path/flatten
   :path/reverse])

(def image-submenu
  [:image/trace])

(defn object-menu []
  {:id :object
   :label [::object "Object"]
   :type :root
   :enabled [::document.subs/entities?]
   :items [:object/to-path
           :object/stroke-to-path
           :separator
           :object/group
           :object/ungroup
           :separator
           :object/lock
           :object/unlock
           :separator
           {:id :align
            :label [::align "Align"]
            :type :sub-menu
            :enabled [::element.subs/not-every-top-level?]
            :items align-submenu}
           {:id :animate
            :label [::animate "Animate"]
            :type :sub-menu
            :enabled [::element.subs/some-selected?]
            :items animate-submenu}
           {:id :boolean
            :label [::boolean-operation "Boolean operation"]
            :type :sub-menu
            :enabled [::element.subs/multiple-selected?]
            :items boolean-submenu}
           :separator
           :object/raise
           :object/lower
           :object/raise-to-top
           :object/lower-to-bottom
           :separator
           {:id :image
            :type :sub-menu
            :label [::image "Image"]
            :enabled [::element.subs/some-selected?]
            :items image-submenu}
           {:id :path
            :label [::path "Path"]
            :type :sub-menu
            :enabled [::element.subs/some-selected?]
            :items path-submenu}]})

(def zoom-submenu
  [:zoom/in
   :zoom/out
   :separator
   :zoom/set-50
   :zoom/set-100
   :zoom/set-200
   :separator
   :zoom/focus-selected
   :zoom/fit-selected
   :zoom/fill-selected])

(defn a11y-submenu []
  (mapv (fn [{:keys [id label]}]
          {:id id
           :label label
           :type :checkbox
           :icon "a11y"
           :active [::a11y.subs/filter-active? id]
           :event [::a11y.events/toggle-active-filter id]})
        @(rf/subscribe [::a11y.subs/filters])))

(defn languages-submenu []
  (->> @(rf/subscribe [::i18n.subs/languages])
       (mapv (fn [[k v]]
               {:id k
                :abbr (:code v)
                :label [k (:locale v)]
                :type :checkbox
                :icon "language"
                :event [::i18n.events/set-user-lang k]
                :active [::i18n.subs/selected-lang? k]}))
       (into [{:id "system"
               :label [::system "System"]
               :type :checkbox
               :icon "language"
               :event [::i18n.events/set-user-lang "system"]
               :active [::i18n.subs/selected-lang? "system"]}])))

(def theme-mode-submenu
  [:theme/set-system-mode
   :theme/set-dark-mode
   :theme/set-light-mode])

(def panel-submenu
  [:panel/toggle-tree
   :panel/toggle-properties
   :panel/toggle-xml
   :panel/toggle-history
   :panel/toggle-repl-history
   :panel/toggle-timeline])

(defn view-menu []
  {:id :view
   :label [::view "View"]
   :type :root
   :items [{:id :zoom
            :label [::zoom "Zoom"]
            :type :sub-menu
            :enabled [::document.subs/entities?]
            :items zoom-submenu}
           {:id :theme-mode
            :label [::theme-mode "Theme mode"]
            :type :sub-menu
            :items theme-mode-submenu}
           {:id :a11y
            :label [::accessibility-filter "Accessibility filter"]
            :type :sub-menu
            :enabled [::document.subs/entities?]
            :items (a11y-submenu)}
           {:id :lang
            :label [::language "Language"]
            :type :sub-menu
            :items (languages-submenu)}
           :separator
           :view/toggle-grid
           :view/toggle-rulers
           :view/toggle-help-bar
           :view/toggle-debug-info
           {:type :separator
            :available [::window.subs/md?]}
           {:id :panel
            :label [::panel "Panel"]
            :type :sub-menu
            :items panel-submenu
            :available [::window.subs/md?]}
           {:type :separator
            :available [::window.subs/md?]}
           :view/toggle-fullscreen]})

(defn help-menu []
  {:id :help
   :label [::help "Help"]
   :type :root
   :items [:app/command-panel
           :separator
           :help/website
           :help/source-code
           :help/license
           :help/changelog
           :help/privacy-policy
           :separator
           :help/submit-issue
           :help/report-errors
           :separator
           :help/about]})

(defn action-menu-item
  [id]
  (when-let [action (action.views/entity id)]
    (cond-> action
      (:active action)
      (assoc :type :checkbox))))

(defmulti menu-item :type)

(defn resolve-item
  [item]
  (when-let [action (cond-> item
                      (keyword? item)
                      action-menu-item)]
    (menu-item action)))

(defmethod menu-item :separator [_]
  [:> Menubar/Separator {:class "menu-separator"}])

(defmethod menu-item :checkbox
  [action]
  [:> Menubar/CheckboxItem
   {:class "menu-checkbox-item inset"
    :on-select (action.views/dispatch action)
    :checked (action.views/checked? action)}
   [:> Menubar/ItemIndicator
    {:class "menu-item-indicator"}
    [views/icon "checkmark"]]
   [:div (action.views/label action)]
   (when @(rf/subscribe [::window.subs/xl?])
     [views/shortcuts action])])

(defmethod menu-item :sub-menu
  [action]
  [:> Menubar/Sub
   [:> Menubar/SubTrigger
    {:class "sub-menu-item menu-item"
     :disabled (action.views/disabled? action)}
    [:div (action.views/label action)]
    [:div.rtl:mr-auto.text-inherit
     {:class "mr-[-1rem] rtl:ml-[-1rem] rtl:scale-x-[-1]"}
     [views/icon "chevron-right"]]]
   [:> Menubar/Portal
    (into [:> Menubar/SubContent
           {:class "menu-content min-w-[45dvw]! sm:min-w-[200px]!
                        max-w-[50dvw]"
            :align "start"
            :loop true
            :on-escape-key-down #(.stopPropagation %)}]
          (map resolve-item (:items action)))]])

(defmethod menu-item :root
  [{:keys [items id]
    :as action}]
  (let [desktop? @(rf/subscribe [::app.subs/desktop?])
        computed-lang @(rf/subscribe [::i18n.subs/lang])
        menubar-indicator? @(rf/subscribe [::menubar.subs/indicator?])]
    [:> Menubar/Menu
     {:value (name id)}
     [:> Menubar/Trigger
      {:class ["button-size py-1 md:min-h-auto md:px-3 xl:py-1.5 flex
                    outline-none select-none items-center justify-center
                    data-[state=open]:bg-overlay leading-none
                    hover:bg-overlay hover:text-foreground-hovered
                    focus:bg-overlay focus:text-foreground-hovered
                    disabled:text-foreground-disabled rounded-sm
                    disabled:pointer-events-none"
               (when desktop? "min-h-auto")]
       :disabled (action.views/disabled? action)}
      [:span
       {:class (when (and menubar-indicator? (= computed-lang "en-US"))
                 "md:first-letter:underline")}
       (or (action.views/label action)
           [views/icon "menu" {:aria-label (i18n.views/t [::menu "Menu"])}])]]
     [:> Menubar/Portal
      (into [:> Menubar/Content
             {:class (when items "menu-content min-w-[45dvw]! sm:min-w-[200px]!
                                  max-w-[45dvw]")
              :align "start"
              :side-offset 4
              :loop true
              :on-escape-key-down #(.stopPropagation %)
              :on-close-auto-focus #(.preventDefault %)}]
            (map resolve-item items))]]))

(defmethod menu-item :default
  [action]
  [:> Menubar/Item
   {:class "menu-item"
    :on-select (action.views/dispatch action)
    :disabled (action.views/disabled? action)}
   [:div (action.views/label action)]
   (when @(rf/subscribe [::window.subs/xl?])
     [views/shortcuts action])])

(defn submenus []
  [(file-menu)
   (edit-menu)
   (object-menu)
   (view-menu)
   (help-menu)])

(defn mobile-root []
  [{:id :root
    :type :root
    :items (mapv #(assoc % :type :sub-menu) (submenus))}])

(defn root []
  (let [active-menu @(rf/subscribe [::menubar.subs/active-menu])
        xl? @(rf/subscribe [::window.subs/xl?])]
    (->> (if xl?
           (submenus)
           (mobile-root))
         (map resolve-item)
         (into [:> Menubar/Root
                {:class "flex overflow-hidden"
                 :on-key-down #(.stopPropagation %)
                 :value (when active-menu (name active-menu))
                 :on-value-change #(if (empty? %)
                                     (rf/dispatch [::menubar.events/deactivate])
                                     (rf/dispatch [::menubar.events/activate
                                                   (keyword %)]))}]))))
