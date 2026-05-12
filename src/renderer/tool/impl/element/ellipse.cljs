(ns renderer.tool.impl.element.ellipse
  "https://www.w3.org/TR/SVG/shapes.html#EllipseElement"
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
   [renderer.utils.length :as utils.length]))

(hierarchy/derive! ::ellipse ::tool.hierarchy/element)

(defn attributes
  [db]
  (let [[offset-x offset-y] (tool.handlers/snapped-offset db)
        [x y] (tool.handlers/snapped-position db)
        [rx ry] (->> [(abs (- x offset-x)) (abs (- y offset-y))]
                     (mapv utils.length/->fixed))]
    {:rx rx
     :ry ry}))

(defmethod tool.hierarchy/on-drag-start [::ellipse :idle]
  [db _e]
  (let [[x y] (tool.handlers/snapped-position db)
        fill (document.handlers/attr db :fill)
        stroke (document.handlers/attr db :stroke)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :ellipse
                               :attrs (merge (attributes db)
                                             {:cx x
                                              :cy y
                                              :fill fill
                                              :stroke stroke})}))))

(defmethod tool.hierarchy/on-drag [::ellipse :create]
  [db _e]
  (let [attrs (attributes db)
        assoc-attr (fn [el [k v]] (assoc-in el [:attrs k] (str v)))]
    (element.handlers/update-selected db #(reduce assoc-attr % attrs))))

(defmethod tool.hierarchy/on-drag-end [::ellipse :create]
  [db e]
  (-> db
      (history.handlers/finalize (:timestamp e)
                                 [::create-ellipse "Create ellipse"])
      (tool.handlers/deactivate)))

(rf/dispatch [::action.events/register-action
              {:id :tool/ellipse
               :label [::label "Ellipse"]
               :icon "ellipse-tool"
               :event [::tool.events/activate ::ellipse]
               :active [::tool.subs/active? ::ellipse]}])
