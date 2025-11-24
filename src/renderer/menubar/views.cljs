(ns renderer.menubar.views
  (:require
   ["@radix-ui/react-menubar" :as Menubar]
   [re-frame.core :as rf]
   [renderer.a11y.events :as-alias a11y.events]
   [renderer.a11y.subs :as-alias a11y.subs]
   [renderer.app.events :as-alias app.events]
   [renderer.app.subs :as-alias app.subs]
   [renderer.dialog.events :as-alias dialog.events]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.events :as-alias element.events]
   [renderer.element.subs :as-alias element.subs]
   [renderer.error.events :as-alias error.events]
   [renderer.error.subs :as-alias error.subs]
   [renderer.events :as-alias events]
   [renderer.frame.events :as-alias frame.events]
   [renderer.history.events :as-alias history.events]
   [renderer.history.subs :as-alias history.subs]
   [renderer.i18n.events :as-alias i18n.events]
   [renderer.i18n.subs :as-alias i18n.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.menubar.events :as-alias menubar.events]
   [renderer.menubar.subs :as-alias menubar.subs]
   [renderer.panel.events :as-alias panel.events]
   [renderer.panel.subs :as-alias panel.subs]
   [renderer.ruler.events :as-alias ruler.events]
   [renderer.ruler.subs :as-alias ruler.subs]
   [renderer.theme.events :as-alias theme.events]
   [renderer.theme.subs :as-alias theme.subs]
   [renderer.views :as views]
   [renderer.window.events :as-alias window.events]
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
                                   :action [::document.events/open-recent
                                            recent]})))]
    (cond-> recent-items
      (seq recent-items)
      (concat [{:id :divider-1
                :type :separator}
               {:id :clear-recent
                :label [::recent-clear "Clear recent"]
                :icon "delete"
                :action [::document.events/clear-recent]}]))))

(defn export-submenu
  []
  [{:id :export-svg
    :label [::svg "SVG"]
    :icon "export"
    :enabled [::document.subs/entities?]
    :action [::document.events/export "image/svg+xml"]}
   {:id :divider-1
    :type :separator}
   {:id :export-png
    :label [::png "PNG"]
    :icon "export"
    :enabled [::document.subs/entities?]
    :action [::document.events/export "image/png"]}
   {:id :export-jpg
    :label [::jpg "JPG"]
    :icon "export"
    :enabled [::document.subs/entities?]
    :action [::document.events/export "image/jpeg"]}
   {:id :export-webp
    :label [::webp "WEBP"]
    :icon "export"
    :enabled [::document.subs/entities?]
    :action [::document.events/export "image/webp"]}
   {:id :export-gif
    :label [::gif "GIF"]
    :icon "export"
    :enabled [::document.subs/entities?]
    :action [::document.events/export "image/gif"]}])

(defn file-menu
  []
  {:id :file
   :label [::file "File"]
   :type :root
   :items [{:id :new-file
            :label [::new "New"]
            :icon "file"
            :action [::document.events/new]}
           {:id :divider-1
            :type :separator}
           {:id :open-file
            :label [::open "Open..."]
            :icon "folder"
            :action [::document.events/open]}
           {:id :recent
            :label [::recent "Recent"]
            :type :sub-menu
            :enabled [::document.subs/recent?]
            :active [::app.subs/feature? :file-system]
            :items (recent-submenu)}
           {:id :divider-2
            :type :separator}
           {:id :save
            :label [::save "Save"]
            :icon "save"
            :action [::document.events/save]
            :enabled [::document.subs/saveable?]
            :active [::app.subs/feature? :file-system]}
           {:id :save-as
            :label [::save-as "Save as..."]
            :icon "save-as"
            :action [::document.events/save-as]
            :enabled [::document.subs/entities?]
            :active [::app.subs/feature? :file-system]}
           {:id :download
            :icon "download"
            :label [::download "Download"]
            :enabled [::document.subs/entities?]
            :action [::document.events/download]}
           {:id :export
            :label [::export-as "Export as"]
            :type :sub-menu
            :enabled [::document.subs/entities?]
            :items (export-submenu)}
           {:id :divider-3
            :type :separator}
           {:id :print
            :label [::print "Print"]
            :icon "printer"
            :enabled [::document.subs/entities?]
            :action [::document.events/print]}
           {:id :divider-4
            :type :separator}
           {:id :close
            :label [::close "Close"]
            :icon "window-close"
            :enabled [::document.subs/entities?]
            :action [::document.events/close-active]}
           {:id :exit
            :label [::exit "Exit"]
            :icon "exit"
            :action [::window.events/close]}]})

(defn edit-menu
  []
  {:id :edit
   :label [::edit "Edit"]
   :type :root
   :enabled [::document.subs/entities?]
   :items [{:id :undo
            :label [::undo "Undo"]
            :icon "undo"
            :enabled [::history.subs/undos?]
            :action [::history.events/undo]}
           {:id :redo
            :label [::redo "Redo"]
            :icon "redo"
            :enabled [::history.subs/redos?]
            :action [::history.events/redo]}
           {:id :divider-1
            :type :separator}
           {:id :cut
            :label [::cut "Cut"]
            :icon "cut"
            :enabled [::element.subs/some-selected?]
            :action [::element.events/cut]}
           {:id :copy
            :icon "copy"
            :label [::copy "Copy"]
            :enabled [::element.subs/some-selected?]
            :action [::element.events/copy]}
           {:id :paste
            :label [::paste "Paste"]
            :icon "paste"
            :action [::element.events/paste]}
           {:id :paste-in-place
            :icon "paste"
            :label [::paste-in-place "Paste in place"]
            :action [::element.events/paste-in-place]}
           {:id :paste-styles
            :icon "paste"
            :label [::paste-styles "Paste styles"]
            :action [::element.events/paste-styles]}
           {:id :divider-2
            :type :separator}
           {:id :duplicate
            :icon "copy"
            :label [::duplicate "Duplicate"]
            :enabled [::element.subs/some-selected?]
            :action [::element.events/duplicate]}
           {:id :divider-3
            :type :separator}
           {:id :select-all
            :icon "select-all"
            :label [::select-all "Select all"]
            :action [::element.events/select-all]}
           {:id :deselect-all
            :icon "deselect-all"
            :label [::deselect-all "Deselect all"]
            :enabled [::element.subs/some-selected?]
            :action [::element.events/deselect-all]}
           {:id :invert-selection
            :label [::invert-selection "Invert selection"]
            :icon "invert-selection"
            :action [::element.events/invert-selection]}
           {:id :select-same-tags
            :icon "select-same"
            :label [::select-same-tags "Select same tags"]
            :enabled [::element.subs/some-selected?]
            :action [::element.events/select-same-tags]}
           {:id :divider-4
            :type :separator}
           {:id :delete
            :icon "delete"
            :label [::delete "Delete"]
            :enabled [::element.subs/some-selected?]
            :action [::element.events/delete]}]})

(defn align-submenu
  []
  [{:id :align-left
    :label [::align-left "Left"]
    :icon "objects-align-left"
    :enabled [::element.subs/not-every-top-level?]
    :action [::element.events/align :left]}
   {:id :align-center-horizontally
    :label [::align-center-horizontally "Center horizontally"]
    :icon "objects-align-center-horizontal"
    :action [::element.events/align :center-horizontal]}
   {:id :align-right
    :label [::align-right "Right"]
    :icon "objects-align-right"
    :action [::element.events/align :right]}
   {:id :divider-1
    :type :separator}
   {:id :align-top
    :label [::align-top "Top"]
    :icon "objects-align-top"
    :action [::element.events/align :top]}
   {:id :align-center-vertically
    :label [::align-center-vertically "Center vertically"]
    :icon "objects-align-center-vertical"
    :action [::element.events/align :center-vertical]}
   {:id :align-bottom
    :label [::align-bottom "Bottom"]
    :icon "objects-align-bottom"
    :action [::element.events/align :bottom]}])

(defn boolean-submenu
  []
  [{:id :exclude
    :label [::boolean-exclude "Exclude"]
    :icon "exclude"
    :action [::element.events/boolean-operation :exclude]
    :enabled [::element.subs/multiple-selected?]}
   {:id :unite
    :label [::boolean-unite "Unite"]
    :icon "unite"
    :action [::element.events/boolean-operation :unite]
    :enabled [::element.subs/multiple-selected?]}
   {:id :intersect
    :label [::boolean-intersect "Intersect"]
    :icon "intersect"
    :action [::element.events/boolean-operation :intersect]
    :enabled [::element.subs/multiple-selected?]}
   {:id :subtract
    :label [::boolean-subtract "Subtract"]
    :icon "subtract"
    :action [::element.events/boolean-operation :subtract]
    :enabled [::element.subs/multiple-selected?]}
   {:id :divide
    :label [::boolean-divide "Divide"]
    :icon "divide"
    :action [::element.events/boolean-operation :divide]
    :enabled [::element.subs/multiple-selected?]}])

(defn animate-submenu
  []
  [{:id :animate
    :label [::animate "Animate"]
    :icon "animation"
    :action [::element.events/animate :animate {}]}
   {:id :animate-transform
    :label [::animate-transform "Animate Transform"]
    :icon "animation"
    :action [::element.events/animate :animateTransform {}]}
   {:id :animate-motion
    :icon "animation"
    :label [::animate-motion "Animate Motion"]
    :action [::element.events/animate :animateMotion {}]}])

(defn path-submenu
  []
  [{:id :simplify
    :label [::path-simplify "Simplify"]
    :icon "bezier-curve"
    :action [::element.events/manipulate-path :simplify]}
   {:id :smooth
    :label [::path-smooth "Smooth"]
    :icon "bezier-curve"
    :action [::element.events/manipulate-path :smooth]}
   {:id :flatten
    :label [::path-flatten "Flatten"]
    :icon "bezier-curve"
    :action [::element.events/manipulate-path :flatten]}
   {:id :reverse
    :label [::path-reverse "Reverse"]
    :icon "bezier-curve"
    :action [::element.events/manipulate-path :reverse]}])

(defn image-submenu
  []
  [{:id :trace
    :label [::image-trace "Trace"]
    :icon "image"
    :action [::element.events/trace]}])

(defn object-menu
  []
  {:id :object
   :label [::object "Object"]
   :type :root
   :enabled [::document.subs/entities?]
   :items [{:id :to-path
            :label [::object-to-path "Object to path"]
            :icon "bezier-curve"
            :enabled [::element.subs/some-selected?]
            :action [::element.events/->path]}
           {:id :stroke-to-path
            :label [::stroke-to-path "Stroke to path"]
            :icon "bezier-curve"
            :enabled [::element.subs/some-selected?]
            :action [::element.events/stroke->path]}
           {:id :divider-1
            :type :separator}
           {:id :group
            :label [::group "Group"]
            :icon "group"
            :enabled [::element.subs/some-selected?]
            :action [::element.events/group]}
           {:id :ungroup
            :label [::ungroup "Ungroup"]
            :icon "ungroup"
            :enabled [::element.subs/some-selected?]
            :action [::element.events/ungroup]}
           {:id :divider-2
            :type :separator}
           {:id :lock
            :label [::lock "Lock"]
            :icon "lock"
            :enabled [::element.subs/some-selected?]
            :action [::element.events/lock]}
           {:id :unlock
            :label [::unlock "Unlock"]
            :icon "unlock"
            :enabled [::element.subs/some-selected?]
            :action [::element.events/unlock]}
           {:id :divider-3
            :type :separator}
           {:id :path
            :label [::align "Align"]
            :type :sub-menu
            :enabled [::element.subs/not-every-top-level?]
            :items (align-submenu)}
           {:id :boolean
            :label [::animate "Animate"]
            :type :sub-menu
            :enabled [::element.subs/some-selected?]
            :items (animate-submenu)}
           {:id :boolean
            :label [::boolean-operation "Boolean operation"]
            :type :sub-menu
            :enabled [::element.subs/multiple-selected?]
            :items (boolean-submenu)}
           {:id :divider-4
            :type :separator}
           {:id :raise
            :label [::raise "Raise"]
            :icon "bring-forward"
            :enabled [::element.subs/some-selected?]
            :action [::element.events/raise]}
           {:id :lower
            :label [::lower "Lower"]
            :icon "send-backward"
            :enabled [::element.subs/some-selected?]
            :action [::element.events/lower]}
           {:id :raise-to-top
            :label [::raise-to-top "Raise to top"]
            :icon "bring-front"
            :enabled [::element.subs/some-selected?]
            :action [::element.events/raise-to-top]}
           {:id :lower-to-bottom
            :label [::lower-to-bottom "Lower to bottom"]
            :icon "send-back"
            :enabled [::element.subs/some-selected?]
            :action [::element.events/lower-to-bottom]}
           {:id :divider-5
            :type :separator}
           {:id :image
            :type :sub-menu
            :label [::image "Image"]
            :enabled [::element.subs/some-selected?]
            :items (image-submenu)}
           {:id :path
            :label [::path "Path"]
            :type :sub-menu
            :enabled [::element.subs/some-selected?]
            :items (path-submenu)}]})

(defn zoom-submenu
  []
  [{:id :zoom-in
    :label [::zoom-in "In"]
    :icon "zoom-in"
    :action [::frame.events/zoom-in]}
   {:id :zoom-out
    :label [::zoom-out "Out"]
    :icon "zoom-out"
    :action [::frame.events/zoom-out]}
   {:id :divider-1
    :type :separator}
   {:label [::zoom-set-50 "Set to 50%"]
    :id "50"
    :icon "magnifier"
    :action [::frame.events/set-zoom 0.5]}
   {:label [::zoom-set-100 "Set to 100%"]
    :id "100"
    :icon "magnifier"
    :action [::frame.events/set-zoom 1]}
   {:label [::zoom-set-200 "Set to 200%"]
    :id "200"
    :icon "magnifier"
    :action [::frame.events/set-zoom 2]}
   {:id :divider-2
    :type :separator}
   {:label [::zoom-focus-selected "Focus selected"]
    :id "focus-selected"
    :icon "focus"
    :action [::frame.events/focus-selection :original]}
   {:label [::zoom-fit-selected "Fit selected"]
    :id "fit-selected"
    :icon "focus"
    :action [::frame.events/focus-selection :fit]}
   {:label [::zoom-fill-selected "Fill selected"]
    :id "fill-selected"
    :icon "focus"
    :action [::frame.events/focus-selection :fill]}])

(defn a11y-submenu
  []
  (mapv (fn [{:keys [id label]}]
          {:id id
           :label label
           :type :checkbox
           :icon "a11y"
           :checked [::a11y.subs/filter-active? id]
           :action [::a11y.events/toggle-active-filter id]})
        @(rf/subscribe [::a11y.subs/filters])))

(defn languages-submenu
  []
  (->> @(rf/subscribe [::i18n.subs/languages])
       (mapv (fn [[k v]]
               {:id k
                :abbr (:code v)
                :label [k (:locale v)]
                :type :checkbox
                :icon "language"
                :action [::i18n.events/set-user-lang k]
                :checked [::i18n.subs/selected-lang? k]}))
       (into [{:id "system"
               :label [::system "System"]
               :type :checkbox
               :icon "language"
               :action [::i18n.events/set-user-lang "system"]
               :checked [::i18n.subs/selected-lang? "system"]}])))

(def theme-mode-submenu
  [{:id :system
    :type :checkbox
    :label [::system "System"]
    :action [::theme.events/set-mode :system]
    :checked [::theme.subs/selected-mode? :system]
    :icon "system"}
   {:id :dark
    :type :checkbox
    :label [::dark "Dark"]
    :action [::theme.events/set-mode :dark]
    :checked [::theme.subs/selected-mode? :dark]
    :icon "dark"}
   {:id :light
    :type :checkbox
    :label [::light "Light"]
    :action [::theme.events/set-mode :light]
    :checked [::theme.subs/selected-mode? :light]
    :icon "light"}])

(defn panel-submenu
  []
  [{:id :toggle-tree
    :type :checkbox
    :icon "tree"
    :label [::panel-element-tree "Element tree"]
    :checked [::panel.subs/visible? :tree]
    :action [::panel.events/toggle :tree]}
   {:id :toggle-props
    :type :checkbox
    :icon "properties"
    :label [::panel-properties "Properties"]
    :checked [::panel.subs/visible? :properties]
    :action [::panel.events/toggle :properties]}
   {:id :toggle-xml
    :label [::panel-xml-view "XML view"]
    :type :checkbox
    :icon "code"
    :checked [::panel.subs/visible? :xml]
    :action [::panel.events/toggle :xml]}
   {:id :toggle-history
    :label [::panel-history-tree "History tree"]
    :icon "history"
    :type :checkbox
    :checked [::panel.subs/visible? :history]
    :action [::panel.events/toggle :history]}
   {:id :toggle-command-history
    :type :checkbox
    :label [::panel-shell-history "Shell history"]
    :icon "shell"
    :checked [::panel.subs/visible? :repl-history]
    :action [::panel.events/toggle :repl-history]}
   {:id :toggle-timeline-panel
    :type :checkbox
    :label [::panel-timeline-editor "Timeline editor"]
    :icon "timeline"
    :checked [::panel.subs/visible? :timeline]
    :action [::panel.events/toggle :timeline]}])

(defn view-menu
  []
  {:id :view
   :label [::view "View"]
   :type :root
   :items [{:id :zoom
            :label [::zoom "Zoom"]
            :type :sub-menu
            :enabled [::document.subs/entities?]
            :items (zoom-submenu)}
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
           {:id :divider-1
            :type :separator}
           {:id :toggle-grid
            :type :checkbox
            :label [::grid "Grid"]
            :icon "grid"
            :checked [::app.subs/grid]
            :action [::app.events/toggle-grid]}
           {:id :toggle-ruler
            :type :checkbox
            :label [::rulers "Rulers"]
            :icon "ruler-combined"
            :checked [::ruler.subs/visible?]
            :action [::ruler.events/toggle-visible]}
           {:id :help-bar
            :type :checkbox
            :label [::help-bar "Help bar"]
            :icon "info"
            :checked [::app.subs/help-bar]
            :action [::app.events/toggle-help-bar]}
           {:id :toggle-debug-info
            :type :checkbox
            :label [::debug-info "Debug info"]
            :icon "bug"
            :checked [::app.subs/debug-info]
            :action [::app.events/toggle-debug-info]}
           {:id :divider-2
            :type :separator
            :active [::window.subs/md?]}
           {:id :panel
            :label [::panel "Panel"]
            :type :sub-menu
            :items (panel-submenu)
            :active [::window.subs/md?]}
           {:id :divider-3
            :type :separator
            :active [::window.subs/md?]}
           {:id :toggle-fullscreen
            :label [::fullscreen "Fullscreen"]
            :icon "arrow-minimize"
            :type :checkbox
            :checked [::window.subs/fullscreen?]
            :action [::window.events/toggle-fullscreen]
            :active [::app.subs/desktop?]}]})

(defn help-menu
  []
  {:id :help
   :label [::help "Help"]
   :type :root
   :items [{:id :cmdk
            :label [::command-panel "Command panel"]
            :icon "command"
            :action [::dialog.events/show-cmdk]}
           {:id :divider-1
            :type :separator}
           {:id :website
            :label [::website "Website"]
            :icon "earth"
            :action [::events/open-remote-url
                     "https://repath.studio/"]}
           {:id :source-code
            :label [::source-code "Source Code"]
            :icon "commit"
            :action [::events/open-remote-url
                     "https://github.com/repath-studio/repath-studio"]}
           {:id :license
            :label [::license "License"]
            :icon "lgpl"
            :action [::events/open-remote-url
                     "https://github.com/repath-studio/repath-studio/blob/main/LICENSE"]}
           {:id :changelog
            :icon "list"
            :label [::changelog "Changelog"]
            :action [::events/open-remote-url
                     "https://repath.studio/roadmap/changelog/"]}
           {:id :privacy-policy
            :icon "list"
            :label [::privacy-policy "Privacy Policy"]
            :action [::events/open-remote-url
                     "https://repath.studio/policies/privacy/"]}
           {:id :divider-2
            :type :separator}
           {:id :submit-issue
            :icon "warning"
            :label [::submit-an-issue "Submit an issue"]
            :action [::events/open-remote-url
                     "https://github.com/repath-studio/repath-studio/issues/new/choose"]}
           {:id :report-errors
            :icon "bug"
            :type :checkbox
            :label [::report-errors "Report errors automatically"]
            :checked [::error.subs/reporting?]
            :action [::error.events/toggle-reporting]}
           {:id :divider-3
            :type :separator}
           {:id :about
            :icon "info"
            :label [::about "About"]
            :action [::dialog.events/show-about]}]})

(defmulti menu-item :type)

(defmethod menu-item :separator
  [{:keys [active]}]
  (when (or (nil? active) @(rf/subscribe active))
    [:> Menubar/Separator {:class "menu-separator"}]))

(defmethod menu-item :checkbox
  [{:keys [label action checked]}]
  [:> Menubar/CheckboxItem
   {:class "menu-checkbox-item inset"
    :on-select #(rf/dispatch action)
    :checked @(rf/subscribe checked)}
   [:> Menubar/ItemIndicator
    {:class "menu-item-indicator"}
    [views/icon "checkmark"]]
   [:div (i18n.views/t label)]
   [views/shortcuts action]])

(defmethod menu-item :sub-menu
  [{:keys [label items enabled active]}]
  (when (or (nil? active) @(rf/subscribe active))
    [:> Menubar/Sub
     [:> Menubar/SubTrigger
      {:class "sub-menu-item menu-item"
       :disabled (some-> enabled rf/subscribe deref not)}
      [:div (i18n.views/t label)]
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
            (map menu-item items))]]))

(defmethod menu-item :root
  [{:keys [label items id enabled active]}]
  (when (or (nil? active) @(rf/subscribe active))
    (let [desktop? @(rf/subscribe [::app.subs/desktop?])
          computed-lang @(rf/subscribe [::i18n.subs/lang])
          menubar-indicator? @(rf/subscribe [::menubar.subs/indicator?])]
      [:> Menubar/Menu
       {:value (name id)}
       [:> Menubar/Trigger
        {:class ["button-size py-1 md:min-h-auto md:px-3 xl:py-1.5 flex
                outline-none select-none items-center justify-center rounded-sm
                data-[state=open]:bg-overlay leading-none
                hover:bg-overlay hover:text-foreground-hovered
                focus:bg-overlay focus:text-foreground-hovered
                disabled:text-foreground-disabled disabled:pointer-events-none"
                 (when desktop? "min-h-auto")]
         :disabled (some-> enabled rf/subscribe deref not)}
        [:span
         {:class (when (and menubar-indicator? (= computed-lang "en-US"))
                   "md:first-letter:underline")}
         (or (some-> label i18n.views/t)
             [views/icon "menu" {:aria-label (i18n.views/t [::menu "Menu"])}])]]
       [:> Menubar/Portal
        (into [:> Menubar/Content
               {:class (when items "menu-content min-w-[45dvw]!
                                    sm:min-w-[200px]! max-w-[45dvw]")
                :align "start"
                :side-offset 4
                :loop true
                :on-escape-key-down #(.stopPropagation %)
                :on-close-auto-focus #(.preventDefault %)}]
              (map menu-item items))]])))

(defmethod menu-item :default
  [{:keys [label action enabled active]}]
  (when (or (nil? active) @(rf/subscribe active))
    [:> Menubar/Item
     {:class "menu-item"
      :on-select #(rf/dispatch action)
      :disabled (some-> enabled rf/subscribe deref not)}
     [:div (i18n.views/t label)]
     [views/shortcuts action]]))

(defn submenus []
  [(file-menu)
   (edit-menu)
   (object-menu)
   (view-menu)
   (help-menu)])

(defn mobile-root
  []
  [{:id :root
    :type :root
    :items (mapv #(assoc % :type :sub-menu) (submenus))}])

(defn root
  []
  (let [active-menu @(rf/subscribe [::menubar.subs/active-menu])
        xl? @(rf/subscribe [::window.subs/xl?])]
    (->> (if xl?
           (submenus)
           (mobile-root))
         (map menu-item)
         (into [:> Menubar/Root
                {:class "flex overflow-hidden"
                 :on-key-down #(.stopPropagation %)
                 :value (when active-menu (name active-menu))
                 :on-value-change #(if (empty? %)
                                     (rf/dispatch [::menubar.events/deactivate])
                                     (rf/dispatch [::menubar.events/activate
                                                   (keyword %)]))}]))))
