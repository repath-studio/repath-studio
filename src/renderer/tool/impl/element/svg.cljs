(ns renderer.tool.impl.element.svg
  "https://www.w3.org/TR/SVG/struct.html#SVGElement"
  (:require
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.element.handlers :as element.handlers]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.input.handlers :as input.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.key :as utils.key]
   [renderer.utils.length :as utils.length]))

(hierarchy/derive! ::svg ::tool.hierarchy/element)

(defmethod tool.hierarchy/tool-options ::svg [])

(defn create-el
  [db]
  (let [[offset-x offset-y] (tool.handlers/snapped-offset db)
        [x y] (tool.handlers/snapped-position db)
        [width height] (mapv (comp utils.length/->fixed abs)
                             [(- x offset-x) (- y offset-y)])
        origin [(cond-> offset-x (< x offset-x) (- width))
                (cond-> offset-y (< y offset-y) (- height))]
        [x y] (mapv utils.length/->fixed origin)]
    (-> db
        (assoc :last-origin origin)
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :svg
                               :attrs {:x x
                                       :y y
                                       :width width
                                       :height height}}))))

(defn update-el
  [db e]
  (let [pointer-pos (tool.handlers/snapped-position db)
        parent-offset (element.handlers/parent-offset db)
        position (matrix/sub pointer-pos parent-offset)
        origin (matrix/sub (:last-origin db) parent-offset)
        position (cond->> position
                   (input.handlers/snap-to-angle? db e)
                   (input.handlers/snap-angle origin))
        size (matrix/sub position origin)
        [w h] (mapv (comp utils.length/->fixed abs) size)
        new-x (cond-> (first origin)
                (neg? (first size))
                (+ (first size)))
        new-y (cond-> (second origin)
                (neg? (second size))
                (+ (second size)))
        [new-x new-y] (mapv utils.length/->fixed [new-x new-y])]
    (element.handlers/update-selected db #(-> %
                                              (assoc-in [:attrs :x] new-x)
                                              (assoc-in [:attrs :y] new-y)
                                              (assoc-in [:attrs :width] w)
                                              (assoc-in [:attrs :height] h)))))

(defn finalize
  [db e]
  (-> db
      (history.handlers/finalize (:timestamp e)
                                 [::create-svg "Create SVG"])
      (tool.handlers/deactivate)))

(defmethod tool.hierarchy/on-drag-start [::svg :idle]
  [db _e]
  (create-el db))

(defmethod tool.hierarchy/on-pointer-up [::svg :idle]
  [db _e]
  (create-el db))

(defmethod tool.hierarchy/on-drag [::svg :create]
  [db e]
  (update-el db e))

(defmethod tool.hierarchy/on-pointer-down [::svg :create]
  [db e]
  (update-el db e))

(defmethod tool.hierarchy/on-pointer-move [::svg :create]
  [db e]
  (update-el db e))

(defmethod tool.hierarchy/on-drag-end [::svg :create]
  [db e]
  (finalize db e))

(defmethod tool.hierarchy/on-pointer-up [::svg :create]
  [db e]
  (finalize db e))
(rf/dispatch [::action.events/register-action
              {:id :tool/svg
               :label [::label "Svg"]
               :icon "svg"
               :event [::tool.events/activate ::svg]
               :active [::tool.subs/active? ::svg]
               :shortcuts [{:keyCode (utils.key/codes "S")}]}])
