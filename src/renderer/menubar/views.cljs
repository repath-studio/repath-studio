(ns renderer.menubar.views
  (:require
   ["@radix-ui/react-menubar" :as Menubar]
   [re-frame.core :as rf]
   [renderer.a11y.subs :as-alias a11y.subs]
   [renderer.action.views :as action.views]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.subs :as-alias element.subs]
   [renderer.i18n.subs :as-alias i18n.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.menubar.events :as-alias menubar.events]
   [renderer.menubar.subs :as-alias menubar.subs]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(defn recent-submenu []
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
               :document/clear-recent]))))

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
   :actions [:document/new
             :separator
             :document/open
             {:id :recent
              :label [::recent "Recent"]
              :enabled [::document.subs/some-recent?]
              :available [::app.subs/supported-feature? :file-system]
              :actions (recent-submenu)}
             :separator
             :document/save
             :document/save-as
             :document/download
             {:id :export
              :label [::export-as "Export as"]
              :enabled [::document.subs/entities?]
              :actions export-submenu}
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
   :actions (->> [[:history/undo
                   :history/redo]
                  (:actions (action.views/deref-action-group :edit/clipboard))
                  [:element/duplicate]
                  [:element/select-all
                   :element/deselect-all
                   :element/invert-selection
                   :element/select-same-tags]
                  [:element/delete]]
                 (interpose :separator)
                 (flatten))})

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
   :actions [:object/to-path
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
              :enabled [::element.subs/not-every-top-level?]
              :actions (->> [:object/horizontal-alignment
                             :object/vertical-alignment]
                            (map (comp :actions
                                       action.views/deref-action-group))
                            (interpose :separator)
                            (flatten))}
             (action.views/deref-action-group :object/animate)
             (action.views/deref-action-group :object/boolean-operations)
             :separator
             :object/raise
             :object/lower
             :object/raise-to-top
             :object/lower-to-bottom
             :separator
             {:id :image
              :label [::image "Image"]
              :enabled [::element.subs/has-selected-tag? :image]
              :actions image-submenu}
             {:id :path
              :label [::path "Path"]
              :enabled [::element.subs/has-selected-tag? :path]
              :actions path-submenu}]})

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

(defn view-menu []
  {:id :view
   :label [::view "View"]
   :type :root
   :actions [{:id :zoom
              :label [::zoom "Zoom"]
              :enabled [::document.subs/entities?]
              :actions zoom-submenu}
             (action.views/deref-action-group :theme/mode)
             {:id :a11y
              :label [::accessibility-filter "Accessibility filter"]
              :enabled [::document.subs/entities?]
              :actions @(rf/subscribe [::a11y.subs/filter-actions])}
             {:id :lang
              :label [::language "Language"]
              :actions @(rf/subscribe [::i18n.subs/language-actions])}
             :separator
             :view/toggle-grid
             :view/toggle-rulers
             :view/toggle-help-bar
             :view/toggle-debug-info
             {:type :separator
              :available [::window.subs/md?]}
             (action.views/deref-action-group :view/panel)
             {:type :separator
              :available [::app.subs/desktop?]}
             :view/toggle-fullscreen]})

(defn help-menu []
  {:id :help
   :label [::help "Help"]
   :type :root
   :actions [:dialog/command-panel
             :separator
             :help/website
             :help/source-code
             :help/license
             :help/changelog
             :help/privacy-policy
             :separator
             :help/submit-issue
             :error/toggle-reporting
             :separator
             :dialog/about]})

(defmulti menu-item
  (fn [action]
    (cond
      (:type action)
      (:type action)

      (:active action)
      :checkbox

      (:actions action)
      :sub-menu

      :else
      :default)))

(defn resolve-item
  [item]
  (when-let [action (cond-> item
                      (keyword? item)
                      action.views/deref-action)]
    (when (action.views/available? action)
      (menu-item action))))

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
   [:div [action.views/label action]]
   [views/shortcuts action]])

(defmethod menu-item :sub-menu
  [action]
  (when (seq (:actions action))
    [:> Menubar/Sub
     [:> Menubar/SubTrigger
      {:class "sub-menu-item menu-item"
       :disabled (action.views/disabled? action)}
      [:div [action.views/label action]]
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
            (map resolve-item (:actions action)))]]))

(defmethod menu-item :root
  [{:keys [actions id]
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
       (or [action.views/label action]
           [views/icon "menu" {:aria-label (i18n.views/t [::menu "Menu"])}])]]
     [:> Menubar/Portal
      (into [:> Menubar/Content
             {:class (when actions "menu-content min-w-[45dvw]!
                                    sm:min-w-[200px]! max-w-[45dvw]")
              :align "start"
              :side-offset 4
              :loop true
              :on-escape-key-down #(.stopPropagation %)
              :on-close-auto-focus #(.preventDefault %)}]
            (map resolve-item actions))]]))

(defmethod menu-item :default
  [action]
  [:> Menubar/Item
   {:class "menu-item"
    :on-select (action.views/dispatch action)
    :disabled (action.views/disabled? action)}
   [:div [action.views/label action]]
   [views/shortcuts action]])

(defn submenus []
  [(file-menu)
   (edit-menu)
   (object-menu)
   (view-menu)
   (help-menu)])

(defn mobile-root []
  [{:id :root
    :type :root
    :actions (mapv #(assoc % :type :sub-menu) (submenus))}])

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
