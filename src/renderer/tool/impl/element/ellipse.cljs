(ns renderer.tool.impl.element.ellipse
  "https://www.w3.org/TR/SVG/shapes.html#EllipseElement"
  (:require
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.input.handlers :as input.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.length :as utils.length]))

(hierarchy/derive! ::ellipse ::tool.hierarchy/element)

(defn create-el
  [db]
  (let [attrs (-> (document.handlers/attrs db)
                  (select-keys [:stroke :fill :stroke-width]))
        [offset-x offset-y] (tool.handlers/snapped-offset db)
        [x y] (tool.handlers/snapped-position db)
        [rx ry] (->> [(abs (- x offset-x)) (abs (- y offset-y))]
                     (mapv utils.length/->fixed))]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :ellipse
                               :attrs (merge attrs {:rx rx
                                                    :ry ry
                                                    :cx x
                                                    :cy y})}))))

(defn update-el
  [db e]
  (let [pointer-pos (tool.handlers/snapped-position db)
        position (matrix/sub pointer-pos (element.handlers/parent-offset db))
        {:keys [cx cy]} (->> db element.handlers/selected first :attrs)
        center (mapv utils.length/unit->px [cx cy])
        position (cond->> position
                   (input.handlers/snap-to-angle? db e)
                   (input.handlers/snap-angle center))
        radius (matrix/sub center position)
        [rx ry] (mapv (comp utils.length/->fixed abs) radius)]
    (element.handlers/update-selected db #(-> %
                                              (assoc-in [:attrs :rx] rx)
                                              (assoc-in [:attrs :ry] ry)))))

(defn finalize
  [db e]
  (-> db
      (history.handlers/finalize (:timestamp e)
                                 [::create-ellipse "Create ellipse"])
      (tool.handlers/deactivate)))

(defmethod tool.hierarchy/on-drag-start [::ellipse :idle]
  [db _e]
  (create-el db))

(defmethod tool.hierarchy/on-pointer-up [::ellipse :idle]
  [db _e]
  (create-el db))

(defmethod tool.hierarchy/on-drag [::ellipse :create]
  [db e]
  (update-el db e))

(defmethod tool.hierarchy/on-pointer-down [::ellipse :create]
  [db e]
  (update-el db e))

(defmethod tool.hierarchy/on-pointer-move [::ellipse :create]
  [db e]
  (update-el db e))

(defmethod tool.hierarchy/on-drag-end [::ellipse :create]
  [db e]
  (finalize db e))

(defmethod tool.hierarchy/on-pointer-up [::ellipse :create]
  [db e]
  (finalize db e))

(rf/dispatch [::action.events/register-action
              {:id :tool/ellipse
               :label [::label "Ellipse"]
               :icon "ellipse-tool"
               :event [::tool.events/activate ::ellipse]
               :active [::tool.subs/active? ::ellipse]}])
