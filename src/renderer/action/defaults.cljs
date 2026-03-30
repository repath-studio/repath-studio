(ns renderer.action.defaults
  (:require
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
   [renderer.menubar.events :as-alias menubar.events]
   [renderer.panel.events :as-alias panel.events]
   [renderer.panel.subs :as-alias panel.subs]
   [renderer.ruler.events :as-alias ruler.events]
   [renderer.ruler.subs :as-alias ruler.subs]
   [renderer.theme.events :as-alias theme.events]
   [renderer.theme.subs :as-alias theme.subs]
   [renderer.tool.events :as-alias tool.events]
   [renderer.utils.key :as utils.key]
   [renderer.window.events :as-alias window.events]
   [renderer.window.subs :as-alias window.subs]))

(def document-new
  {:id :document/new
   :label [::new "New"]
   :icon "file"
   :event [::document.events/new]
   :shortcuts [{:keyCode (utils.key/codes "N")
                :ctrlKey true}]})

(def document-open
  {:id :document/open
   :label [::open "Open…"]
   :icon "folder"
   :event [::document.events/open]
   :shortcuts [{:keyCode (utils.key/codes "O")
                :ctrlKey true}]})

(def document-save
  {:id :document/save
   :label [::save "Save"]
   :icon "save"
   :event [::document.events/save]
   :shortcuts [{:keyCode (utils.key/codes "S")
                :ctrlKey true}]
   :enabled [::document.subs/saveable?]
   :active [::app.subs/supported-feature? :file-system]})

(def document-save-as
  {:id :document/save-as
   :label [::save-as "Save as…"]
   :icon "save-as"
   :event [::document.events/save-as]
   :shortcuts [{:keyCode (utils.key/codes "S")
                :ctrlKey true
                :shiftKey true}]
   :enabled [::document.subs/entities?]
   :active [::app.subs/supported-feature? :file-system]})

(def document-download
  {:id :document/download
   :label [::download "Download"]
   :icon "download"
   :event [::document.events/download]
   :enabled [::document.subs/entities?]})

(def document-close
  {:id :document/close
   :label [::close "Close"]
   :icon "window-close"
   :event [::document.events/close-active]
   :shortcuts [{:keyCode (utils.key/codes "W")
                :ctrlKey true}]
   :enabled [::document.subs/entities?]})

(def document-close-all
  {:id :document/close-all
   :label [::close-all "Close all"]
   :event [::document.events/close-all]
   :shortcuts [{:keyCode (utils.key/codes "W")
                :ctrlKey true
                :altKey true}]})

(def document-print
  {:id :document/print
   :label [::print "Print"]
   :icon "printer"
   :event [::document.events/print]
   :enabled [::document.subs/entities?]})

(def export-svg
  {:id :export/svg
   :label [::svg "SVG"]
   :icon "export"
   :event [::document.events/export "image/svg+xml"]
   :enabled [::document.subs/entities?]})

(def export-png
  {:id :export/png
   :label [::png "PNG"]
   :icon "export"
   :event [::document.events/export "image/png"]
   :enabled [::document.subs/entities?]})

(def export-jpg
  {:id :export/jpg
   :label [::jpg "JPG"]
   :icon "export"
   :event [::document.events/export "image/jpeg"]
   :enabled [::document.subs/entities?]})

(def export-webp
  {:id :export/webp
   :label [::webp "WEBP"]
   :icon "export"
   :event [::document.events/export "image/webp"]
   :enabled [::document.subs/entities?]})

(def export-gif
  {:id :export/gif
   :label [::gif "GIF"]
   :icon "export"
   :event [::document.events/export "image/gif"]
   :enabled [::document.subs/entities?]})

(def history-undo
  {:id :history/undo
   :label [::undo "Undo"]
   :icon "undo"
   :event [::history.events/undo]
   :shortcuts [{:keyCode (utils.key/codes "Z")
                :ctrlKey true}]
   :enabled [::history.subs/undos?]})

(def history-redo
  {:id :history/redo
   :label [::redo "Redo"]
   :icon "redo"
   :event [::history.events/redo]
   :shortcuts [{:keyCode (utils.key/codes "Z")
                :ctrlKey true
                :shiftKey true}
               {:keyCode (utils.key/codes "Y")
                :ctrlKey true}]
   :enabled [::history.subs/redos?]})

(def clipboard-cut
  {:id :clipboard/cut
   :label [::cut "Cut"]
   :icon "cut"
   :event [::element.events/cut]
   :shortcuts [{:keyCode (utils.key/codes "X")
                :ctrlKey true}]
   :enabled [::element.subs/some-selected?]})

(def clipboard-copy
  {:id :clipboard/copy
   :label [::copy "Copy"]
   :icon "copy"
   :event [::element.events/copy]
   :shortcuts [{:keyCode (utils.key/codes "C")
                :ctrlKey true}]
   :enabled [::element.subs/some-selected?]})

(def clipboard-paste
  {:id :clipboard/paste
   :label [::paste "Paste"]
   :icon "paste"
   :event [::element.events/paste]
   :shortcuts [{:keyCode (utils.key/codes "V")
                :ctrlKey true}]})

(def clipboard-paste-in-place
  {:id :clipboard/paste-in-place
   :label [::paste-in-place "Paste in place"]
   :icon "paste"
   :event [::element.events/paste-in-place]
   :shortcuts [{:keyCode (utils.key/codes "V")
                :ctrlKey true
                :altKey true}]})

(def clipboard-paste-styles
  {:id :clipboard/paste-styles
   :label [::paste-styles "Paste styles"]
   :icon "paste"
   :event [::element.events/paste-styles]
   :shortcuts [{:keyCode (utils.key/codes "V")
                :ctrlKey true
                :shiftKey true}]})

(def element-duplicate
  {:id :element/duplicate
   :label [::duplicate "Duplicate"]
   :icon "copy"
   :event [::element.events/duplicate]
   :shortcuts [{:keyCode (utils.key/codes "D")
                :ctrlKey true}]
   :enabled [::element.subs/some-selected?]})

(def element-delete
  {:id :element/delete
   :label [::delete "Delete"]
   :icon "delete"
   :event [::element.events/delete]
   :shortcuts [{:keyCode (utils.key/codes "DELETE")}
               {:keyCode (utils.key/codes "BACKSPACE")}]
   :enabled [::element.subs/some-selected?]})

(def element-select-all
  {:id :element/select-all
   :label [::select-all "Select all"]
   :icon "select-all"
   :event [::element.events/select-all]
   :shortcuts [{:keyCode (utils.key/codes "A")
                :ctrlKey true}]})

(def element-deselect-all
  {:id :element/deselect-all
   :label [::deselect-all "Deselect all"]
   :icon "deselect-all"
   :event [::element.events/deselect-all]
   :enabled [::element.subs/some-selected?]})

(def element-invert-selection
  {:id :element/invert-selection
   :label [::invert-selection "Invert selection"]
   :icon "invert-selection"
   :event [::element.events/invert-selection]})

(def element-select-same-tags
  {:id :element/select-same-tags
   :label [::select-same-tags "Select same tags"]
   :icon "select-same"
   :event [::element.events/select-same-tags]
   :shortcuts [{:keyCode (utils.key/codes "A")
                :ctrlKey true
                :shiftKey true}]
   :enabled [::element.subs/some-selected?]})

(def object-to-path
  {:id :object/to-path
   :label [::object-to-path "Object to path"]
   :icon "bezier-curve"
   :event [::element.events/->path]
   :shortcuts [{:keyCode (utils.key/codes "P")
                :ctrlKey true
                :shiftKey true}]
   :enabled [::element.subs/some-selected?]})

(def object-stroke-to-path
  {:id :object/stroke-to-path
   :label [::stroke-to-path "Stroke to path"]
   :icon "bezier-curve"
   :event [::element.events/stroke->path]
   :shortcuts [{:keyCode (utils.key/codes "P")
                :ctrlKey true
                :altKey true}]
   :enabled [::element.subs/some-selected?]})

(def object-group
  {:id :object/group
   :label [::group "Group"]
   :icon "group"
   :event [::element.events/group]
   :shortcuts [{:keyCode (utils.key/codes "G")
                :ctrlKey true}]
   :enabled [::element.subs/some-selected?]})

(def object-ungroup
  {:id :object/ungroup
   :label [::ungroup "Ungroup"]
   :icon "ungroup"
   :event [::element.events/ungroup]
   :shortcuts [{:keyCode (utils.key/codes "G")
                :ctrlKey true
                :shiftKey true}]
   :enabled [::element.subs/some-selected?]})

(def object-lock
  {:id :object/lock
   :label [::lock "Lock"]
   :icon "lock"
   :event [::element.events/lock]
   :shortcuts [{:keyCode (utils.key/codes "L")
                :ctrlKey true}]
   :enabled [::element.subs/some-selected?]})

(def object-unlock
  {:id :object/unlock
   :label [::unlock "Unlock"]
   :icon "unlock"
   :event [::element.events/unlock]
   :shortcuts [{:keyCode (utils.key/codes "L")
                :ctrlKey true
                :shiftKey true}]
   :enabled [::element.subs/some-selected?]})

(def object-raise
  {:id :object/raise
   :label [::raise "Raise"]
   :icon "bring-forward"
   :event [::element.events/raise]
   :shortcuts [{:keyCode (utils.key/codes "PAGE_UP")}]
   :enabled [::element.subs/some-selected?]})

(def object-lower
  {:id :object/lower
   :label [::lower "Lower"]
   :icon "send-backward"
   :event [::element.events/lower]
   :shortcuts [{:keyCode (utils.key/codes "PAGE_DOWN")}]
   :enabled [::element.subs/some-selected?]})

(def object-raise-to-top
  {:id :object/raise-to-top
   :label [::raise-to-top "Raise to top"]
   :icon "bring-front"
   :event [::element.events/raise-to-top]
   :shortcuts [{:keyCode (utils.key/codes "HOME")}]
   :enabled [::element.subs/some-selected?]})

(def object-lower-to-bottom
  {:id :object/lower-to-bottom
   :label [::lower-to-bottom "Lower to bottom"]
   :icon "send-back"
   :event [::element.events/lower-to-bottom]
   :shortcuts [{:keyCode (utils.key/codes "END")}]
   :enabled [::element.subs/some-selected?]})

(def align-left
  {:id :align/left
   :label [::align-left "Left"]
   :icon "objects-align-left"
   :event [::element.events/align :left]
   :enabled [::element.subs/not-every-top-level?]})

(def align-center-horizontal
  {:id :align/center-horizontal
   :label [::center-horizontally "Center horizontally"]
   :icon "objects-align-center-horizontal"
   :event [::element.events/align :center-horizontal]
   :enabled [::element.subs/not-every-top-level?]})

(def align-right
  {:id :align/right
   :label [::align-right "Right"]
   :icon "objects-align-right"
   :event [::element.events/align :right]
   :enabled [::element.subs/not-every-top-level?]})

(def align-top
  {:id :align/top
   :label [::align-top "Top"]
   :icon "objects-align-top"
   :event [::element.events/align :top]
   :enabled [::element.subs/not-every-top-level?]})

(def align-center-vertical
  {:id :align/center-vertical
   :label [::center-vertically "Center vertically"]
   :icon "objects-align-center-vertical"
   :event [::element.events/align :center-vertical]
   :enabled [::element.subs/not-every-top-level?]})

(def align-bottom
  {:id :align/bottom
   :label [::align-bottom "Bottom"]
   :icon "objects-align-bottom"
   :event [::element.events/align :bottom]
   :enabled [::element.subs/not-every-top-level?]})

(def boolean-exclude
  {:id :boolean/exclude
   :label [::boolean-exclude "Exclude"]
   :icon "exclude"
   :event [::element.events/boolean-operation :exclude]
   :shortcuts [{:keyCode (utils.key/codes "E")
                :ctrlKey true}]
   :enabled [::element.subs/multiple-selected?]})

(def boolean-unite
  {:id :boolean/unite
   :label [::boolean-unite "Unite"]
   :icon "unite"
   :event [::element.events/boolean-operation :unite]
   :shortcuts [{:keyCode (utils.key/codes "U")
                :ctrlKey true}]
   :enabled [::element.subs/multiple-selected?]})

(def boolean-intersect
  {:id :boolean/intersect
   :label [::boolean-intersect "Intersect"]
   :icon "intersect"
   :event [::element.events/boolean-operation :intersect]
   :shortcuts [{:keyCode (utils.key/codes "I")
                :ctrlKey true}]
   :enabled [::element.subs/multiple-selected?]})

(def boolean-subtract
  {:id :boolean/subtract
   :label [::boolean-subtract "Subtract"]
   :icon "subtract"
   :event [::element.events/boolean-operation :subtract]
   :shortcuts [{:keyCode (utils.key/codes "BACKSLASH")
                :ctrlKey true}]
   :enabled [::element.subs/multiple-selected?]})

(def boolean-divide
  {:id :boolean/divide
   :label [::boolean-divide "Divide"]
   :icon "divide"
   :event [::element.events/boolean-operation :divide]
   :shortcuts [{:keyCode (utils.key/codes "SLASH")
                :ctrlKey true}]
   :enabled [::element.subs/multiple-selected?]})

(def animate
  {:id :animate/animate
   :label [::animate "Animate"]
   :icon "animation"
   :event [::element.events/animate :animate]})

(def animate-transform
  {:id :animate/transform
   :label [::animate-transform "Animate Transform"]
   :icon "animation"
   :event [::element.events/animate :animateTransform]})

(def animate-motion
  {:id :animate/motion
   :label [::animate-motion "Animate Motion"]
   :icon "animation"
   :event [::element.events/animate :animateMotion]})

(def path-simplify
  {:id :path/simplify
   :label [::path-simplify "Simplify"]
   :icon "bezier-curve"
   :event [::element.events/manipulate-path :simplify]})

(def path-smooth
  {:id :path/smooth
   :label [::path-smooth "Smooth"]
   :icon "bezier-curve"
   :event [::element.events/manipulate-path :smooth]})

(def path-flatten
  {:id :path/flatten
   :label [::path-flatten "Flatten"]
   :icon "bezier-curve"
   :event [::element.events/manipulate-path :flatten]})

(def path-reverse
  {:id :path/reverse
   :label [::path-reverse "Reverse"]
   :icon "bezier-curve"
   :event [::element.events/manipulate-path :reverse]})

(def image-trace
  {:id :image/trace
   :label [::image-trace "Trace"]
   :icon "image"
   :event [::element.events/trace]})

(def zoom-in
  {:id :zoom/in
   :label [::zoom-in "In"]
   :icon "zoom-in"
   :event [::frame.events/zoom-in]
   :shortcuts [{:keyCode (utils.key/codes "EQUALS")}]})

(def zoom-out
  {:id :zoom/out
   :label [::zoom-out "Out"]
   :icon "zoom-out"
   :event [::frame.events/zoom-out]
   :shortcuts [{:keyCode (utils.key/codes "DASH")}]})

(def zoom-set-50
  {:id :zoom/set-50
   :label [::zoom-set-50 "Set to 50%"]
   :icon "magnifier"
   :event [::frame.events/set-zoom 0.5]})

(def zoom-set-100
  {:id :zoom/set-100
   :label [::zoom-set-100 "Set to 100%"]
   :icon "magnifier"
   :event [::frame.events/set-zoom 1]})

(def zoom-set-200
  {:id :zoom/set-200
   :label [::zoom-set-200 "Set to 200%"]
   :icon "magnifier"
   :event [::frame.events/set-zoom 2]})

(def zoom-focus-selected
  {:id :zoom/focus-selected
   :label [::zoom-focus-selected "Focus selected"]
   :icon "focus"
   :event [::frame.events/focus-selection :original]
   :shortcuts [{:keyCode (utils.key/codes "ONE")}]})

(def zoom-fit-selected
  {:id :zoom/fit-selected
   :label [::zoom-fit-selected "Fit selected"]
   :icon "focus"
   :event [::frame.events/focus-selection :fit]
   :shortcuts [{:keyCode (utils.key/codes "TWO")}]})

(def zoom-fill-selected
  {:id :zoom/fill-selected
   :label [::zoom-fill-selected "Fill selected"]
   :icon "focus"
   :event [::frame.events/focus-selection :fill]
   :shortcuts [{:keyCode (utils.key/codes "THREE")}]})

(def toggle-grid
  {:id :view/toggle-grid
   :label [::grid "Grid"]
   :icon "grid"
   :event [::app.events/toggle-grid]
   :checked [::app.subs/grid]
   :shortcuts [{:keyCode (utils.key/codes "PERIOD")
                :ctrlKey true}]})

(def toggle-rulers
  {:id :view/toggle-rulers
   :label [::rulers "Rulers"]
   :icon "ruler-combined"
   :event [::ruler.events/toggle-visible]
   :checked [::ruler.subs/visible?]
   :shortcuts [{:keyCode (utils.key/codes "R")
                :ctrlKey true}]})

(def toggle-help-bar
  {:id :view/toggle-help-bar
   :label [::help-bar "Help bar"]
   :icon "info"
   :checked [::app.subs/help-bar]
   :event [::app.events/toggle-help-bar]})

(def toggle-debug-info
  {:id :view/toggle-debug-info
   :label [::debug-info "Debug info"]
   :icon "bug"
   :event [::app.events/toggle-debug-info]
   :checked [::app.subs/debug-info]
   :shortcuts [{:keyCode (utils.key/codes "D")
                :ctrlKey true
                :shiftKey true}]})

(def toggle-fullscreen
  {:id :view/toggle-fullscreen
   :label [::fullscreen "Fullscreen"]
   :icon "arrow-minimize"
   :event [::window.events/toggle-fullscreen]
   :shortcuts [{:keyCode (utils.key/codes "F11")}]
   :active [::app.subs/desktop?]
   :checked [::window.subs/fullscreen?]})

(def toggle-panel-tree
  {:id :panel/toggle-tree
   :label [::panel-element-tree "Element tree"]
   :icon "tree"
   :event [::panel.events/toggle :tree]
   :checked [::panel.subs/visible? :tree]
   :shortcuts [{:keyCode (utils.key/codes "T")
                :ctrlKey true}]})

(def toggle-panel-properties
  {:id :panel/toggle-properties
   :label [::panel-properties "Properties"]
   :icon "properties"
   :event [::panel.events/toggle :properties]
   :checked [::panel.subs/visible? :properties]
   :shortcuts [{:keyCode (utils.key/codes "P")
                :ctrlKey true}]})

(def toggle-panel-xml
  {:id :panel/toggle-xml
   :label [::panel-xml-view "XML view"]
   :icon "code"
   :event [::panel.events/toggle :xml]
   :checked [::panel.subs/visible? :xml]})

(def toggle-panel-history
  {:id :panel/toggle-history
   :label [::panel-history-tree "History tree"]
   :icon "history"
   :event [::panel.events/toggle :history]
   :checked [::panel.subs/visible? :history]
   :shortcuts [{:keyCode (utils.key/codes "H")
                :ctrlKey true}]})

(def toggle-panel-repl-history
  {:id :panel/toggle-repl-history
   :label [::panel-shell-history "Shell history"]
   :icon "shell"
   :event [::panel.events/toggle :repl-history]
   :checked [::panel.subs/visible? :repl-history]})

(def toggle-panel-timeline
  {:id :panel/toggle-timeline
   :label [::panel-timeline-editor "Timeline editor"]
   :icon "timeline"
   :event [::panel.events/toggle :timeline]
   :checked [::panel.subs/visible? :timeline]})

(def window-close
  {:id :window/close
   :label [::exit "Exit"]
   :icon "exit"
   :event [::window.events/close]
   :shortcuts [{:keyCode (utils.key/codes "Q")
                :ctrlKey true}]})

(def command-panel
  {:id :app/command-panel
   :label [::command-panel "Command panel"]
   :icon "command"
   :event [::dialog.events/show-cmdk]
   :shortcuts [{:keyCode (utils.key/codes "F1")}
               {:keyCode (utils.key/codes "K")
                :ctrlKey true}]})

(def help-website
  {:id :help/website
   :label [::website "Website"]
   :icon "earth"
   :event [::events/open-remote-url "https://repath.studio/"]})

(def help-source-code
  {:id :help/source-code
   :label [::source-code "Source Code"]
   :icon "commit"
   :event [::events/open-remote-url "https://github.com/repath-studio/repath-studio"]})

(def help-license
  {:id :help/license
   :label [::license "License"]
   :icon "lgpl"
   :event [::events/open-remote-url "https://github.com/repath-studio/repath-studio/blob/main/LICENSE"]})

(def help-changelog
  {:id :help/changelog
   :label [::changelog "Changelog"]
   :icon "list"
   :event [::events/open-remote-url "https://repath.studio/roadmap/changelog/"]})

(def help-privacy-policy
  {:id :help/privacy-policy
   :label [::privacy-policy "Privacy Policy"]
   :icon "list"
   :event [::events/open-remote-url "https://repath.studio/policies/privacy/"]})

(def help-submit-issue
  {:id :help/submit-issue
   :label [::submit-an-issue "Submit an issue"]
   :icon "warning"
   :event [::events/open-remote-url "https://github.com/repath-studio/repath-studio/issues/new/choose"]})

(def help-report-errors
  {:id :help/report-errors
   :icon "bug"
   :type :checkbox
   :label [::report-errors "Report errors automatically"]
   :checked [::error.subs/reporting?]
   :event [::error.events/toggle-reporting]})

(def help-about
  {:id :help/about
   :label [::about "About"]
   :icon "info"
   :event [::dialog.events/show-about]})

(def tool-cancel
  {:id :tool/cancel
   :label [::cancel "Cancel"]
   :event [::tool.events/cancel]
   :shortcuts [{:keyCode (utils.key/codes "ESC")}]})

(def menubar-activate-file
  {:id :menubar/activate-file
   :label [::menubar-file "File menu"]
   :event [::menubar.events/activate :file]
   :shortcuts [{:keyCode (utils.key/codes "F")
                :altKey true}]})

(def menubar-activate-edit
  {:id :menubar/activate-edit
   :label [::menubar-edit "Edit menu"]
   :event [::menubar.events/activate :edit]
   :shortcuts [{:keyCode (utils.key/codes "E")
                :altKey true}]})

(def menubar-activate-object
  {:id :menubar/activate-object
   :label [::menubar-object "Object menu"]
   :event [::menubar.events/activate :object]
   :shortcuts [{:keyCode (utils.key/codes "O")
                :altKey true}]})

(def menubar-activate-view
  {:id :menubar/activate-view
   :label [::menubar-view "View menu"]
   :event [::menubar.events/activate :view]
   :shortcuts [{:keyCode (utils.key/codes "V")
                :altKey true}]})

(def menubar-activate-help
  {:id :menubar/activate-help
   :label [::menubar-help "Help menu"]
   :event [::menubar.events/activate :help]
   :shortcuts [{:keyCode (utils.key/codes "H")
                :altKey true}]})

(def tool-activate-edit
  {:id :tool/activate-edit
   :label [::tool-edit "Edit tool"]
   :event [::tool.events/activate :edit]
   :shortcuts [{:keyCode (utils.key/codes "E")}]})

(def tool-activate-circle
  {:id :tool/activate-circle
   :label [::tool-circle "Circle tool"]
   :event [::tool.events/activate :circle]
   :shortcuts [{:keyCode (utils.key/codes "C")}]})

(def tool-activate-line
  {:id :tool/activate-line
   :label [::tool-line "Line tool"]
   :event [::tool.events/activate :line]
   :shortcuts [{:keyCode (utils.key/codes "L")}]})

(def tool-activate-text
  {:id :tool/activate-text
   :label [::tool-text "Text tool"]
   :event [::tool.events/activate :text]
   :shortcuts [{:keyCode (utils.key/codes "T")}]})

(def tool-activate-pan
  {:id :tool/activate-pan
   :label [::tool-pan "Pan tool"]
   :event [::tool.events/activate :pan]
   :shortcuts [{:keyCode (utils.key/codes "P")}]})

(def tool-activate-zoom
  {:id :tool/activate-zoom
   :label [::tool-zoom "Zoom tool"]
   :event [::tool.events/activate :zoom]
   :shortcuts [{:keyCode (utils.key/codes "Z")}]})

(def tool-activate-rect
  {:id :tool/activate-rect
   :label [::tool-rect "Rectangle tool"]
   :event [::tool.events/activate :rect]
   :shortcuts [{:keyCode (utils.key/codes "R")}]})

(def tool-activate-transform
  {:id :tool/activate-transform
   :label [::tool-transform "Transform tool"]
   :event [::tool.events/activate :transform]
   :shortcuts [{:keyCode (utils.key/codes "S")}]})

(def tool-activate-fill
  {:id :tool/activate-fill
   :label [::tool-fill "Fill tool"]
   :event [::tool.events/activate :fill]
   :shortcuts [{:keyCode (utils.key/codes "F")}]})

(def set-dark-theme-mode
  {:id :theme/set-dark-mode
   :label [::dark "Dark"]
   :icon "dark"
   :event [::theme.events/set-mode :dark]
   :checked [::theme.subs/selected-mode? :dark]})

(def set-light-theme-mode
  {:id :theme/set-light-mode
   :label [::light "Light"]
   :icon "light"
   :event [::theme.events/set-mode :light]
   :checked [::theme.subs/selected-mode? :light]})

(def set-system-theme-mode
  {:id :theme/set-system-mode
   :label [::system "System"]
   :icon "system"
   :event [::theme.events/set-mode :system]
   :checked [::theme.subs/selected-mode? :system]})

(def actions
  [document-new
   document-open
   document-save
   document-save-as
   document-download
   document-close
   document-close-all
   document-print
   export-svg
   export-png
   export-jpg
   export-webp
   export-gif
   history-undo
   history-redo
   clipboard-cut
   clipboard-copy
   clipboard-paste
   clipboard-paste-in-place
   clipboard-paste-styles
   element-duplicate
   element-delete
   element-select-all
   element-deselect-all
   element-invert-selection
   element-select-same-tags
   object-to-path
   object-stroke-to-path
   object-group
   object-ungroup
   object-lock
   object-unlock
   object-raise
   object-lower
   object-raise-to-top
   object-lower-to-bottom
   align-left
   align-center-horizontal
   align-right
   align-top
   align-center-vertical
   align-bottom
   boolean-exclude
   boolean-unite
   boolean-intersect
   boolean-subtract
   boolean-divide
   animate
   animate-transform
   animate-motion
   path-simplify
   path-smooth
   path-flatten
   path-reverse
   image-trace
   zoom-in
   zoom-out
   zoom-set-50
   zoom-set-100
   zoom-set-200
   zoom-focus-selected
   zoom-fit-selected
   zoom-fill-selected
   toggle-grid
   toggle-rulers
   toggle-help-bar
   toggle-debug-info
   toggle-fullscreen
   toggle-panel-tree
   toggle-panel-properties
   toggle-panel-xml
   toggle-panel-history
   toggle-panel-repl-history
   toggle-panel-timeline
   window-close
   command-panel
   help-website
   help-source-code
   help-license
   help-changelog
   help-privacy-policy
   help-submit-issue
   help-report-errors
   help-about
   tool-cancel
   menubar-activate-file
   menubar-activate-edit
   menubar-activate-object
   menubar-activate-view
   menubar-activate-help
   tool-activate-edit
   tool-activate-circle
   tool-activate-line
   tool-activate-text
   tool-activate-pan
   tool-activate-zoom
   tool-activate-rect
   tool-activate-transform
   tool-activate-fill
   set-system-theme-mode
   set-dark-theme-mode
   set-light-theme-mode])

(def registry
  (zipmap (map :id actions) actions))
