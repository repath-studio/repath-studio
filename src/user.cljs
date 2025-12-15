(ns user
  (:require
   [clojure.math]
   [clojure.string :as string]
   [config :as config]
   [re-frame.core :as rf]
   [re-frame.db :as rf.db]
   [renderer.a11y.events :as-alias a11y.events]
   [renderer.document.events :as-alias document.events]
   [renderer.element.events :as-alias element.events]
   [renderer.history.events :as-alias history.events]
   [renderer.i18n.events :as-alias i18n.events]
   [renderer.window.events :as-alias window.events]))

(defn ^:export translate
  "Moves the selected elements."
  ([offset]
   (rf/dispatch-sync [::element.events/translate offset]))
  ([x y]
   (translate [x y])))

(defn ^:export place
  "Moves the selected elements."
  ([pos]
   (rf/dispatch-sync [::element.events/place pos]))
  ([x y]
   (place [x y])))

(defn ^:export scale
  "Scales the selected elements."
  ([ratio]
   (rf/dispatch-sync [::element.events/scale (if (number? ratio)
                                               [ratio ratio]
                                               ratio)]))
  ([x y]
   (rf/dispatch-sync [::element.events/scale [x y]])))

(defn ^:export fill
  "Fills the selected elements."
  [color]
  (rf/dispatch-sync [::element.events/set-attr :fill color]))

(defn ^:export delete
  "Deletes the selected elements."
  []
  (rf/dispatch-sync [::element.events/delete]))

(defn ^:export copy
  "Copies the selected elements."
  []
  (rf/dispatch-sync [::element.events/copy]))

(defn ^:export paste
  "Pastes the selected elements."
  []
  (rf/dispatch-sync [::element.events/paste]))

(defn ^:export paste-in-place
  "Pastes the selected elements in place."
  []
  (rf/dispatch-sync [::element.events/paste-in-place]))

(defn ^:export duplicate
  "Duplicates the selected elements."
  []
  (rf/dispatch-sync [::element.events/duplicate]))

(defn ^:export create
  "Creates a new element."
  [el]
  (rf/dispatch-sync [::element.events/add el]))

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
  (rf/dispatch-sync [::element.events/set-attr k v]))

(defn ^:export set-fill
  "Sets the fill color of the editor."
  [color]
  (rf/dispatch-sync [::document.events/set-attr :fill color]))

(defn ^:export set-stroke
  "Sets the stroke color of the editor."
  [color]
  (rf/dispatch-sync [::document.events/set-attr :stroke color]))

(defn ^:export db
  []
  @rf.db/app-db)

(defn ^:export document
  []
  (get-in (db) [:documents (:active-document (db))]))

(defn ^:export elements
  []
  (:elements (document)))

(defn ^:export raise
  "Raises the selected elements."
  []
  (rf/dispatch-sync [::element.events/raise]))

(defn ^:export lower
  "Lowers the selected elements."
  []
  (rf/dispatch-sync [::element.events/lower]))

(defn ^:export group
  "Groups the selected elements."
  []
  (rf/dispatch-sync [::element.events/group]))

(defn ^:export ungroup
  "Ungroups the selected elements."
  []
  (rf/dispatch-sync [::element.events/ungroup]))

(defn ^:export select-all
  "Selects all elements."
  []
  (rf/dispatch-sync [::element.events/select-all]))

(defn ^:export deselect-all
  "Deselects all elements."
  []
  (rf/dispatch-sync [::element.events/deselect-all]))

(defn ^:export ->path
  "Converts the selected elements to paths."
  []
  (rf/dispatch-sync [::element.events/->path]))

(defn ^:export stroke->path
  "Converts the selected elements' stroke to paths."
  []
  (rf/dispatch-sync [::element.events/stroke->path]))

(defn ^:export align
  "Aligns the selected elements to the provided direction.
   Accepted directions
   :left :right :top :bottom :center-vertical :center-horizontal"
  [direction]
  (rf/dispatch-sync [::element.events/align direction]))

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
  "Animates the selected elements."
  ([]
   (animate {}))
  ([attrs]
   (animate :animate attrs))
  ([tag attrs]
   (rf/dispatch-sync [::element.events/animate tag attrs])))

(defn ^:export undo
  "Goes back in history."
  ([]
   (rf/dispatch-sync [::history.events/undo]))
  ([steps]
   (rf/dispatch-sync [::history.events/undo-by steps])))

(defn ^:export redo
  "Goes forward in history."
  ([]
   (rf/dispatch-sync [::history.events/redo]))
  ([steps]
   (rf/dispatch-sync [::history.events/redo-by steps])))

(defn ^:export unite
  "Unites the selected elements."
  []
  (rf/dispatch-sync [::element.events/boolean-operation :unite]))

(defn ^:export intersect
  "Intersects the selected elements."
  []
  (rf/dispatch-sync [::element.events/boolean-operation :intersect]))

(defn ^:export subtract
  "Subtracts the selected elements."
  []
  (rf/dispatch-sync [::element.events/boolean-operation :subtract]))

(defn ^:export exclude
  "Excludes the selected elements."
  []
  (rf/dispatch-sync [::element.events/boolean-operation :exclude]))

(defn ^:export div
  "Divides the selected elements."
  []
  (rf/dispatch-sync [::element.events/boolean-operation :divide]))

(defn ^:export exit
  "Closes the application."
  []
  (rf/dispatch-sync [::window.events/close]))

(defn ^:export register-a11y-filter
  "Registers an accessibility filter."
  [a11y-filter]
  (rf/dispatch-sync [::a11y.events/register-filter a11y-filter]))

(defn ^:export deregister-a11y-filter
  "Deregisters an accessibility filter."
  [id]
  (rf/dispatch-sync [::a11y.events/deregister-filter id]))

(defn ^:export register-language
  "Registers a language."
  [language]
  (rf/dispatch-sync [::i18n.events/register-language language]))

(defn ^:export set-translation
  "Sets a translation for a language."
  [lang-id k v]
  (rf/dispatch-sync [::i18n.events/set-translation lang-id k v]))

(defn ^:export deregister-language
  "Deregisters a language."
  [id]
  (rf/dispatch-sync [::i18n.events/deregister-language id]))

(defn ^:export help
  "Lists available functions."
  []
  (doseq [x (sort-by str (vals (ns-publics 'user)))]
    (print (:name (meta x)) " - " (:doc (meta x))))
  "")

;; Expose all commands to global namespace.
;; We might have to redeclare those functions for each language.
(doseq [command (vals (ns-publics 'user))]
  (aset js/window (:name (meta command)) (.call ^js (.-val command))))

(def ^:export version config/version)

(comment
  (dotimes [x 25]
    (circle [(+ (* x 30) 40) (+ (* (js/Math.sin x) 10) 200)]
            10
            {:fill (str "hsl(" (* x 10) " ,50% , 50%)")}))
  #())
