(ns user
  "This is the default REPL namespace.
   We use function inline schemas that are parsed and displayed as docs."
  (:require
   [clojure.math]
   [clojure.string :as string]
   [config :as config]
   [malli.experimental :as mx]
   [re-frame.core :as rf]
   [re-frame.db :as rf.db]
   [renderer.a11y.events :as-alias a11y.events]
   [renderer.action.events :as-alias action.events]
   [renderer.document.events :as-alias document.events]
   [renderer.element.events :as-alias element.events]
   [renderer.history.events :as-alias history.events]
   [renderer.i18n.events :as-alias i18n.events]
   [renderer.icon.events :as-alias icon.events]
   [renderer.shell.events :as-alias shell.events]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.window.events :as-alias window.events]))

(mx/defn clear
  "Clears the shell output."
  []
  (rf/dispatch [::shell.events/clear-items]))

(mx/defn translate
  "Moves the selected elements.

   Arguments:
   - `x`: The x-axis offset.
   - `y`: The y-axis offset."
  [x :- :float y :- :float]
  (rf/dispatch [::element.events/translate [x y]]))

(mx/defn place
  "Places the selected elements to a specific position.

   Arguments:
   - `x`: The x-axis coordinate.
   - `y`: The y-axis coordinate."
  [x :- :float y :- :float]
  (rf/dispatch [::element.events/place [x y]]))

(mx/defn scale
  "Scales the selected elements.

   Arguments:
   - `ratio`: The scale ratio (for both axis).
   - `x`:     The x-axis ratio
   - `y`:     The y-axis ratio"
  ([ratio :- :float]
   (rf/dispatch [::element.events/scale (if (number? ratio)
                                          [ratio ratio]
                                          ratio)]))
  ([x :- :float y :- :float]
   (rf/dispatch [::element.events/scale [x y]])))

(mx/defn fill
  "Fills the selected elements.

   Arguments:
   - `color`: The color of the fill."
  [color :- :string]
  (rf/dispatch [::element.events/set-attr :fill color]))

(mx/defn del
  "Deletes the selected elements."
  []
  (rf/dispatch [::element.events/delete]))

(mx/defn copy
  "Copies the selected elements."
  []
  (rf/dispatch [::element.events/copy]))

(mx/defn paste
  "Pastes the selected elements."
  []
  (rf/dispatch [::element.events/paste]))

(mx/defn paste-in-place
  "Pastes the selected elements in place."
  []
  (rf/dispatch [::element.events/paste-in-place]))

(mx/defn duplicate
  "Duplicates the selected elements."
  []
  (rf/dispatch [::element.events/duplicate]))

(mx/defn create
  "Creates a new element."
  [el]
  (rf/dispatch [::element.events/add el]))

(mx/defn circle
  "Creates a circle.

   Arguments:
   - `cx`:    The x-axis coordinate of the center point.
   - `cy`:    The y-axis coordinate of the center point.
   - `r`:     The radius of the circle.
   - `attrs`: Optional map of attributes."
  ([cx :- :float cy :- :float r :- :float]
   (circle cx cy r {}))
  ([cx :- :float cy :- :float r :- :float attrs :- :map]
   (create {:tag :circle
            :attrs (merge {:cx cx
                           :cy cy
                           :r r} attrs)})))

(mx/defn rect
  "Creates a rectangle.

   Arguments:
   - `x`:      The x-axis coordinate.
   - `y`:      The y-axis coordinate.
   - `width`:  The horizontal length of the rectangle.
   - `height`: The vertical length of the rectangle.
   - `attrs`:  Optional map of attributes."
  ([x :- :float y :- :float width :- :float height :- :float]
   (rect x y width height {}))
  ([x :- :float y :- :float width :- :float height :- :float attrs :- :map]
   (create {:tag :rect
            :attrs (merge {:x x
                           :y y
                           :width width
                           :height height} attrs)})))

(mx/defn line
  "Creates a line.

   Arguments:
   - `x1`:    The first x-coordinate of the line.
   - `y1`:    The first y-coordinate of the line.
   - `x2`:    The second x-coordinate of the line.
   - `y2`:    The second y-coordinate of the line.
   - `attrs`: Optional map of attributes."
  ([x1 :- :float y1 :- :float x2 :- :float y2 :- :float]
   (line x1 y1 x2 y2 {}))
  ([x1 :- :float y1 :- :float x2 :- :float y2 :- :float attrs :- :map]
   (create {:tag :line
            :attrs (merge {:x1 x1
                           :y1 y1
                           :x2 x2
                           :y2 y2
                           :stroke "#000000"} attrs)})))

(mx/defn polygon
  "Creates a polygon.

   Arguments:
   - `points`: The list of points of the polygon. Each point is a
               pair of X and Y coordinates in the user coordinate system.
   - `attrs`:  Optional map of attributes."
  ([points :- [:vector :float]]
   (polygon points {}))
  ([points :- [:vector :float] attrs :- :map]
   (create {:tag :polygon
            :attrs (merge {:points (string/join " " (flatten points))}
                          attrs)})))

(mx/defn polyline
  "Creates a polyline.

   Arguments:
   - `points`: The list of points of the polyline. Each point is a
               pair of X and Y coordinates in the user coordinate system.
   - `attrs`:  Optional map of attributes."
  ([points :- [:vector :float]]
   (polyline points {}))
  ([points :- [:vector :float] attrs :- :map]
   (create {:tag :polyline
            :attrs (merge {:points (string/join " " (flatten points))}
                          attrs)})))

(mx/defn path
  "Creates a path.

   Arguments:
   - `path-commands`: The path commands that define the path to be drawn.
   - `attrs`:         Optional map of attributes."
  ([path-commands :- [:vector :string]]
   (path path-commands {}))
  ([path-commands :- [:vector :string] attrs :- :map]
   (create {:tag :path
            :attrs (merge {:d (string/join " " (flatten path-commands))}
                          attrs)})))

(mx/defn image
  "Creates an image.

   Arguments:
   - `x`:      The x-axis coordinate.
   - `y`:      The y-axis coordinate.
   - `width`:  The horizontal length of the image.
   - `height`: The vertical length of the image.
   - `href`:   The link to the image resource as a reference URL.
   - `attrs`:  Optional map of attributes."
  ([x :- :float y :- :float width :- :float height :- :float
    href :- :string]
   (image x y width height href {}))
  ([x :- :float y :- :float width :- :float height :- :float href :- :string
    attrs :- :map]
   (create {:tag :image
            :attrs (merge {:x x
                           :y y
                           :width width
                           :height height
                           :href href} attrs)})))

(mx/defn text
  "Creates a text element.

   Arguments:
   - `x`:       The x-axis coordinate.
   - `y`:       The y-axis coordinate.
   - `content`: The text content.
   - `attrs`:   Optional map of attributes."
  ([x :- :float y :- :float content :- :string]
   (text x y content {}))
  ([x :- :float y :- :float content :- :string attrs :- :map]
   (create {:tag :text
            :content content
            :attrs (merge {:x x
                           :y y} attrs)})))

(mx/defn set-attr
  "Sets the attribute of the selected elements.

   Arguments:
   - `k`: The name (key) of the attribute.
   - `v`: The value of the attribute."
  [k v]
  (rf/dispatch [::element.events/set-attr (keyword k) v]))

(mx/defn set-fill
  "Sets the fill color of the editor.

   Arguments:
   - `color`: The color of the fill."
  [color]
  (rf/dispatch [::document.events/set-attr :fill color]))

(mx/defn set-stroke
  "Sets the stroke color of the editor.

   Arguments:
   - `color`: The color of the stroke."
  [color]
  (rf/dispatch [::document.events/set-attr :stroke color]))

(mx/defn db
  "Returns the application database."
  []
  @rf.db/app-db)

(mx/defn document
  "Returns the active document."
  []
  (get-in (db) [:documents (:active-document (db))]))

(mx/defn elements
  "Returns the elements of the active document."
  []
  (:elements (document)))

(mx/defn raise
  "Raises the selected elements."
  []
  (rf/dispatch [::element.events/raise]))

(mx/defn lower
  "Lowers the selected elements."
  []
  (rf/dispatch [::element.events/lower]))

(mx/defn raise-to-top
  "Raises the selected elements to top."
  []
  (rf/dispatch [::element.events/raise-to-top]))

(mx/defn lower-to-bottom
  "Lowers the selected elements to bottom."
  []
  (rf/dispatch [::element.events/lower-to-bottom]))

(mx/defn group
  "Groups the selected elements."
  []
  (rf/dispatch [::element.events/group]))

(mx/defn ungroup
  "Ungroups the selected elements."
  []
  (rf/dispatch [::element.events/ungroup]))

(mx/defn select-all
  "Selects all elements."
  []
  (rf/dispatch [::element.events/select-all]))

(mx/defn deselect-all
  "Deselects all elements."
  []
  (rf/dispatch [::element.events/deselect-all]))

(mx/defn element-to-path
  "Converts the selected elements to paths."
  []
  (rf/dispatch [::element.events/->path]))

(mx/defn stroke-to-path
  "Converts the selected elements' stroke to paths."
  []
  (rf/dispatch [::element.events/stroke->path]))

(mx/defn align-left
  "Aligns the selected elements to the left."
  []
  (rf/dispatch [::element.events/align-left]))

(mx/defn align-right
  "Aligns the selected elements to the right."
  []
  (rf/dispatch [::element.events/align-right]))

(mx/defn align-top
  "Aligns the selected elements to the top."
  []
  (rf/dispatch [::element.events/align-top]))

(mx/defn align-bottom
  "Aligns the selected elements to the bottom."
  []
  (rf/dispatch [::element.events/align-bottom]))

(mx/defn center-vertically
  "Aligns the selected elements to the vertical center."
  []
  (rf/dispatch [::element.events/center-vertically]))

(mx/defn center-horizontally
  "Aligns the selected elements to the horizontal center."
  []
  (rf/dispatch [::element.events/center-horizontally]))

(mx/defn animate
  "Animates an attribute of the selected elements over time."
  ([]
   (animate {}))
  ([attrs :- :map]
   (rf/dispatch [::element.events/animate attrs])))

(mx/defn animate-transform
  "Animates a transformation attribute of the selected elements to control
   translation, scaling, rotation, and/or skewing."
  ([]
   (animate-transform {}))
  ([attrs :- :map]
   (rf/dispatch [::element.events/animate-transform attrs])))

(mx/defn animate-motion
  "Animates the selected elements along a motion path."
  ([]
   (animate-motion {}))
  ([attrs :- :map]
   (rf/dispatch [::element.events/animate-motion attrs])))

(mx/defn undo
  "Goes back in history."
  ([]
   (rf/dispatch [::history.events/undo]))
  ([steps]
   (rf/dispatch [::history.events/undo-by steps])))

(mx/defn redo
  "Goes forward in history."
  ([]
   (rf/dispatch [::history.events/redo]))
  ([steps]
   (rf/dispatch [::history.events/redo-by steps])))

(mx/defn unite
  "Unites the selected elements."
  []
  (rf/dispatch [::element.events/boolean-unite]))

(mx/defn intersect
  "Intersects the selected elements."
  []
  (rf/dispatch [::element.events/boolean-intersect]))

(mx/defn subtract
  "Subtracts the selected elements."
  []
  (rf/dispatch [::element.events/boolean-subtract]))

(mx/defn exclude
  "Excludes the selected elements."
  []
  (rf/dispatch [::element.events/boolean-exclude]))

(mx/defn div
  "Divides the selected elements."
  []
  (rf/dispatch [::element.events/boolean-divide]))

(mx/defn exit
  "Closes the application."
  []
  (rf/dispatch [::window.events/close]))

(mx/defn register-icon
  "Registers an icon."
  [icon]
  (rf/dispatch [::icon.events/register-icon icon]))

(mx/defn deregister-icon
  "Deregisters an icon."
  [id]
  (rf/dispatch [::icon.events/deregister-icon id]))

(mx/defn register-accessibility-filter
  "Registers an accessibility filter."
  [a11y-filter]
  (rf/dispatch [::a11y.events/register-filter a11y-filter]))

(mx/defn deregister-accessibility-filter
  "Deregisters an accessibility filter."
  [id]
  (rf/dispatch [::a11y.events/deregister-filter id]))

(mx/defn languages
  "Returns the registered languages."
  []
  (-> (db) :languages keys sort))

(mx/defn register-language
  "Registers a language."
  [language]
  (rf/dispatch [::i18n.events/register-language language]))

(mx/defn set-translation
  "Sets a translation for a language."
  [lang-id k v]
  (rf/dispatch [::i18n.events/set-translation lang-id (keyword k) v]))

(mx/defn deregister-language
  "Deregisters a language."
  [id]
  (rf/dispatch [::i18n.events/deregister-language (keyword id)]))

(mx/defn actions
  "Returns the registered actions."
  []
  (-> (db) :actions keys sort))

(mx/defn register-action
  "Registers an action."
  [action]
  (rf/dispatch [::action.events/register-action action]))

(mx/defn deregister-action
  "Deregisters an action."
  [id]
  (rf/dispatch [::action.events/deregister-action id]))

(mx/defn action-groups
  "Returns the registered action groups."
  []
  (-> (db) :action-groups keys sort))

(mx/defn register-action-group
  "Registers an action group."
  [action-group]
  (rf/dispatch [::action.events/register-action-group action-group]))

(mx/defn deregister-action-group
  "Deregisters an action group."
  [id]
  (rf/dispatch [::action.events/deregister-action-group id]))

(mx/defn add-action-to-group
  "Adds an action to an action group."
  [group-id action-id]
  (rf/dispatch [::action.events/add-action-to-group
                (keyword group-id)
                (keyword action-id)]))

(mx/defn remove-action-from-group
  "Removes an action from an action group."
  [group-id action-id]
  (rf/dispatch [::action.events/remove-action-from-group
                (keyword group-id)
                (keyword action-id)]))

(mx/defn help
  "Lists the available functions or returns help for a specific command."
  ([]
   (doseq [x (sort-by str (vals (ns-publics 'user)))]
     (help (:name (meta x)))))
  ([command :- :string]
   (let [lang (-> (db) :shell :active-language)]
     (shell.hierarchy/help lang command))))

(mx/defn version
  "The application version."
  []
  config/version)

(comment
  (dotimes [x 25]
    (circle (+ (* x 30) 40) (+ (* (js/Math.sin x) 10) 200) 10
            {:fill (str "hsl(" (* x 10) " ,50% , 50%)")}))

  (register-action {:id :history/undo-twice
                    :label [:history/undo-twice "Undo twice"]
                    :icon "undo"
                    :event [:renderer.history.events/undo-by 2]
                    :shortcuts [{:keyCode 90
                                 :ctrlKey true
                                 :altKey true}]
                    :enabled [:renderer.history.subs/undos?]})

  (register-icon {:id "dot"
                  :path "M 12.982 8.5 A 4.482 4.482 0 0 1 8.5 12.982 A 4.482
                         4.482 0 0 1 4.018 8.5 A 4.482 4.482 0 0 1 12.982 8.5
                         z"})

  (register-language {:id "im-LA"
                      :dir "ltr"
                      :locale "Imaginary language"
                      :code "LA"
                      :dictionary {}})

  (set-translation "en-US" :renderer.menubar.views.file "New File")

  (register-accessibility-filter {:id :blur-x3
                                  :tag :feGaussianBlur
                                  :label [[:a11y-filter/blur-x3 "blur-x3"]]
                                  :attrs {:in "SourceGraphic"
                                          :type "matrix"
                                          :stdDeviation "3"}})

  (add-action-to-group :object/index-operations :object/lock)

  #())
