(ns user
  (:require
   [clojure.math]
   [clojure.string :as string]
   [config :as config]
   [malli.error :as m.error]
   [re-frame.core :as rf]
   [re-frame.db :as rf.db]
   [renderer.a11y.db :as a11y.db]
   [renderer.a11y.events :as-alias a11y.events]
   [renderer.action.db :as action.db]
   [renderer.action.events :as-alias action.events]
   [renderer.document.events :as-alias document.events]
   [renderer.element.db :as element.db]
   [renderer.element.events :as-alias element.events]
   [renderer.history.events :as-alias history.events]
   [renderer.i18n.db :as i18n.db]
   [renderer.i18n.events :as-alias i18n.events]
   [renderer.icon.db :as icon.db]
   [renderer.icon.events :as-alias icon.events]
   [renderer.shell.events :as-alias shell.events]
   [renderer.window.events :as-alias window.events]))

(defn ^:export clear
  "Clears the shell output."
  []
  (rf/dispatch [::shell.events/clear-items]))

(defn ^:export translate
  "Moves the selected elements."
  ([offset]
   (rf/dispatch [::element.events/translate offset]))
  ([x y]
   (translate [x y])))

(defn ^:export place
  "Places the selected elements to a specific position."
  ([pos]
   (rf/dispatch [::element.events/place pos]))
  ([x y]
   (place [x y])))

(defn ^:export scale
  "Scales the selected elements."
  ([ratio]
   (rf/dispatch [::element.events/scale (if (number? ratio)
                                          [ratio ratio]
                                          ratio)]))
  ([x y]
   (rf/dispatch [::element.events/scale [x y]])))

(defn ^:export fill
  "Fills the selected elements."
  [color]
  (rf/dispatch [::element.events/set-attr :fill color]))

(defn ^:export delete
  "Deletes the selected elements."
  []
  (rf/dispatch [::element.events/delete]))

(defn ^:export copy
  "Copies the selected elements."
  []
  (rf/dispatch [::element.events/copy]))

(defn ^:export paste
  "Pastes the selected elements."
  []
  (rf/dispatch [::element.events/paste]))

(defn ^:export paste-in-place
  "Pastes the selected elements in place."
  []
  (rf/dispatch [::element.events/paste-in-place]))

(defn ^:export duplicate
  "Duplicates the selected elements."
  []
  (rf/dispatch [::element.events/duplicate]))

(defn ^:export create
  "Creates a new element."
  [el]
  (if (element.db/valid? (update el :attrs update-vals #(when % (str %))))
    (rf/dispatch [::element.events/add el])
    (let [error (-> el element.db/explain m.error/humanize)]
      (throw (ex-info (str "Invalid element: " error) {:element el})))))

(defn ^:export circle
  "Creates a circle."
  [[cx cy] r & {:as attrs}]
  (create {:tag :circle
           :attrs (merge {:cx cx
                          :cy cy
                          :r r} attrs)}))

(defn ^:export rect
  "Creates a rectangle."
  [x y width height & {:as attrs}]
  (create {:tag :rect
           :attrs (merge {:x x
                          :y y
                          :width width
                          :height height} attrs)}))

(defn ^:export line
  "Creates a line."
  [[x1 y1] [x2 y2] & {:as attrs}]
  (create {:tag :line
           :attrs (merge {:x1 x1
                          :y1 y1
                          :x2 x2
                          :y2 y2
                          :stroke "#000000"} attrs)}))

(defn ^:export polygon
  "Creates a polygon."
  [points & {:as attrs}]
  (create {:tag :polygon
           :attrs (merge {:points (string/join " " (flatten points))}
                         attrs)}))

(defn ^:export polyline
  "Creates a polyline."
  [points & {:as attrs}]
  (create {:tag :polyline
           :attrs (merge {:points (string/join " " (flatten points))}
                         attrs)}))

(defn ^:export path
  "Creates a path."
  [path-commands & {:as attrs}]
  (create {:path (merge {:d (string/join " " (flatten path-commands))}
                        attrs)}))

(defn ^:export image
  "Creates an image."
  [[x y] width height href & {:as attrs}]
  (create {:tag :image
           :attrs (merge {:x x
                          :y y
                          :width width
                          :height height
                          :href href} attrs)}))

(defn ^:export text
  "Creates a text element."
  [[x y] content & {:as attrs}]
  (create {:tag :text
           :content content
           :attrs (merge {:x x
                          :y y} attrs)}))

(defn ^:export set-attr
  "Sets the attribute of the selected elements."
  [k v]
  (rf/dispatch [::element.events/set-attr (keyword k) v]))

(defn ^:export set-fill
  "Sets the fill color of the editor."
  [color]
  (rf/dispatch [::document.events/set-attr :fill color]))

(defn ^:export set-stroke
  "Sets the stroke color of the editor."
  [color]
  (rf/dispatch [::document.events/set-attr :stroke color]))

(defn ^:export db
  "Returns the application database."
  []
  @rf.db/app-db)

(defn ^:export document
  "Returns the active document."
  []
  (get-in (db) [:documents (:active-document (db))]))

(defn ^:export elements
  "Returns the elements of the active document."
  []
  (:elements (document)))

(defn ^:export raise
  "Raises the selected elements."
  []
  (rf/dispatch [::element.events/raise]))

(defn ^:export lower
  "Lowers the selected elements."
  []
  (rf/dispatch [::element.events/lower]))

(defn ^:export group
  "Groups the selected elements."
  []
  (rf/dispatch [::element.events/group]))

(defn ^:export ungroup
  "Ungroups the selected elements."
  []
  (rf/dispatch [::element.events/ungroup]))

(defn ^:export select-all
  "Selects all elements."
  []
  (rf/dispatch [::element.events/select-all]))

(defn ^:export deselect-all
  "Deselects all elements."
  []
  (rf/dispatch [::element.events/deselect-all]))

(defn ^:export element-to-path
  "Converts the selected elements to paths."
  []
  (rf/dispatch [::element.events/->path]))

(defn ^:export stroke-to-path
  "Converts the selected elements' stroke to paths."
  []
  (rf/dispatch [::element.events/stroke->path]))

(defn ^:export align
  "Aligns the selected elements to the provided direction.
   Accepted directions
   :left :right :top :bottom :center-vertical :center-horizontal"
  [direction]
  (rf/dispatch [::element.events/align direction]))

(defn ^:export al
  "Aligns the selected elements to the left."
  []
  (align :left))

(defn ^:export ar
  "Aligns the selected elements to the right."
  []
  (align :right))

(defn ^:export at
  "Aligns the selected elements to the top."
  []
  (align :top))

(defn ^:export ab
  "Aligns the selected elements to the bottom."
  []
  (align :bottom))

(defn ^:export acv
  "Aligns the selected elements to the vertical center."
  []
  (align :center-vertical))

(defn ^:export ach
  "Aligns the selected elements to the horizontal center."
  []
  (align :center-horizontal))

(defn ^:export animate
  "Animates an attribute of the selected elements over time."
  [& {:as attrs}]
  (rf/dispatch [::element.events/animate :animate attrs]))

(defn ^:export animate-transform
  "Animates a transformation attribute of the selected elements to control
   translation, scaling, rotation, and/or skewing."
  [& {:as attrs}]
  (rf/dispatch [::element.events/animate :animateTransform attrs]))

(defn ^:export animate-motion
  "Animates the selected elements along a motion path."
  [& {:as attrs}]
  (rf/dispatch [::element.events/animate :animateMotion attrs]))

(defn ^:export undo
  "Goes back in history."
  ([]
   (rf/dispatch [::history.events/undo]))
  ([steps]
   (rf/dispatch [::history.events/undo-by steps])))

(defn ^:export redo
  "Goes forward in history."
  ([]
   (rf/dispatch [::history.events/redo]))
  ([steps]
   (rf/dispatch [::history.events/redo-by steps])))

(defn ^:export unite
  "Unites the selected elements."
  []
  (rf/dispatch [::element.events/boolean-operation :unite]))

(defn ^:export intersect
  "Intersects the selected elements."
  []
  (rf/dispatch [::element.events/boolean-operation :intersect]))

(defn ^:export subtract
  "Subtracts the selected elements."
  []
  (rf/dispatch [::element.events/boolean-operation :subtract]))

(defn ^:export exclude
  "Excludes the selected elements."
  []
  (rf/dispatch [::element.events/boolean-operation :exclude]))

(defn ^:export div
  "Divides the selected elements."
  []
  (rf/dispatch [::element.events/boolean-operation :divide]))

(defn ^:export exit
  "Closes the application."
  []
  (rf/dispatch [::window.events/close]))

(defn ^:export register-icon
  "Registers an icon."
  [icon]
  (if (icon.db/valid-icon? icon)
    (rf/dispatch [::icon.events/register-icon icon])
    (let [error (-> icon icon.db/explain-icon m.error/humanize)]
      (throw (ex-info (str "Invalid icon: " error) {:icon icon})))))

(defn ^:export deregister-icon
  "Deregisters an icon."
  [id]
  (rf/dispatch [::icon.events/deregister-icon id]))

(defn ^:export register-accessibility-filter
  "Registers an accessibility filter."
  [a11y-filter]
  (if (a11y.db/valid-filter? a11y-filter)
    (rf/dispatch [::a11y.events/register-filter a11y-filter])
    (let [error (-> a11y-filter a11y.db/explain-filter m.error/humanize)]
      (throw (ex-info (str "Invalid a11y filter: " error)
                      {:a11y-filter a11y-filter})))))

(defn ^:export deregister-accessibility-filter
  "Deregisters an accessibility filter."
  [id]
  (rf/dispatch [::a11y.events/deregister-filter id]))

(defn ^:export languages
  "Returns the registered languages."
  []
  (-> (db) :languages keys sort))

(defn ^:export register-language
  "Registers a language."
  [language]
  (if (i18n.db/valid-language? language)
    (rf/dispatch [::i18n.events/register-language language])
    (let [error (-> language
                    i18n.db/explain-language
                    m.error/humanize)]
      (throw (ex-info (str "Invalid language: " error)
                      {:language language})))))

(defn ^:export set-translation
  "Sets a translation for a language."
  [lang-id k v]
  (rf/dispatch [::i18n.events/set-translation lang-id (keyword k) v]))

(defn ^:export deregister-language
  "Deregisters a language."
  [id]
  (rf/dispatch [::i18n.events/deregister-language (keyword id)]))

(defn ^:export actions
  "Returns the registered actions."
  []
  (-> (db) :actions keys sort))

(defn ^:export register-action
  "Registers an action."
  [action]
  (if (action.db/valid-action? action)
    (rf/dispatch [::action.events/register-action action])
    (let [error (-> action action.db/explain-action m.error/humanize)]
      (throw (ex-info (str "Invalid action: " error) {:action action})))))

(defn ^:export deregister-action
  "Deregisters an action."
  [id]
  (rf/dispatch [::action.events/deregister-action id]))

(defn ^:export action-groups
  "Returns the registered action groups."
  []
  (-> (db) :action-groups keys sort))

(defn ^:export register-action-group
  "Registers an action group."
  [action-group]
  (if (action.db/valid-action-group? action-group)
    (rf/dispatch [::action.events/register-action-group action-group])
    (let [error (-> action-group
                    action.db/explain-action-group
                    m.error/humanize)]
      (throw (ex-info (str "Invalid action group: " error)
                      {:group action-group})))))

(defn ^:export deregister-action-group
  "Deregisters an action group."
  [id]
  (rf/dispatch [::action.events/deregister-action-group id]))

(defn ^:export add-action-to-group
  "Adds an action to an action group."
  [group-id action-id]
  (rf/dispatch [::action.events/add-action-to-group
                (keyword group-id)
                (keyword action-id)]))

(defn ^:export remove-action-from-group
  "Removes an action from an action group."
  [group-id action-id]
  (rf/dispatch [::action.events/remove-action-from-group
                (keyword group-id)
                (keyword action-id)]))

(defn ^:export help
  "Lists the available functions or returns help for a specific command."
  ([]
   (doseq [x (sort-by str (vals (ns-publics 'user)))]
     (help (:name (meta x)))))
  ([command]
   (if-let [f (get (ns-publics 'user) (symbol command))]
     (print (:name (meta f)) " - " (:doc (meta f)))
     (println "Command not found:" command))))

(defn ^:export version
  "The application version."
  []
  config/version)

(comment
  (dotimes [x 25]
    (circle [(+ (* x 30) 40) (+ (* (js/Math.sin x) 10) 200)]
            10
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
