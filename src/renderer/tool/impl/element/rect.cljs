(ns renderer.tool.impl.element.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement"
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.key :as utils.key]
   [renderer.utils.length :as utils.length]))

(hierarchy/derive! ::rect ::tool.hierarchy/element)

(defn attributes
  [db]
  (let [[offset-x offset-y] (tool.handlers/snapped-offset db)
        [x y] (tool.handlers/snapped-position db)
        [width height] (mapv abs [(- x offset-x) (- y offset-y)])]
    {:x (utils.length/->fixed (cond-> offset-x (< x offset-x) (- width)))
     :y (utils.length/->fixed (cond-> offset-y (< y offset-y) (- height)))
     :width (utils.length/->fixed width)
     :height (utils.length/->fixed height)}))

(defmethod tool.hierarchy/on-drag-start [::rect :idle]
  [db _e]
  (let [fill (document.handlers/attr db :fill)
        stroke (document.handlers/attr db :stroke)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :rect
                               :attrs (merge (attributes db)
                                             {:fill fill
                                              :stroke stroke})}))))

(defmethod tool.hierarchy/on-drag [::rect :create]
  [db _e]
  (let [attrs (attributes db)
        assoc-attr (fn [el [k v]] (assoc-in el [:attrs k] (str v)))
        [min-x min-y] (element.handlers/parent-offset db)]
    (-> db
        (element.handlers/update-selected #(reduce assoc-attr % attrs))
        (element.handlers/translate [(- min-x) (- min-y)]))))

(defmethod tool.hierarchy/on-drag-end [::rect :create]
  [db e]
  (-> db
      (history.handlers/finalize (:timestamp e)
                                 [::create-rectangle "Create rectangle"])
      (tool.handlers/deactivate)))

(rf/dispatch [::action.events/register-action
              {:id :tool/rect
               :label [::label "Rectangle"]
               :icon "rectangle-tool"
               :event [::tool.events/activate ::rect]
               :active [::tool.subs/active? ::rect]
               :shortcuts [{:keyCode (utils.key/codes "R")}]}])
