(ns renderer.tool.impl.element.polygon
  "https://www.w3.org/TR/SVG/shapes.html#PolygonElement"
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.element.poly :as poly]
   [renderer.tool.subs :as-alias tool.subs]))

(hierarchy/derive! ::polygon ::tool.hierarchy/poly)

(defmethod tool.hierarchy/on-double-click [::polygon :create]
  [db e]
  (-> db
      (poly/drop-last-point)
      (history.handlers/finalize (:timestamp e)
                                 [::create-polygon "Create polygon"])
      (tool.handlers/deactivate)))

(defmethod tool.hierarchy/on-pointer-up [::polygon :idle]
  [db _e]
  (let [initial-point (tool.handlers/snapped-position db)
        attrs (-> (document.handlers/attrs db)
                  (select-keys [:stroke :fill :stroke-width])
                  (assoc :points (string/join " " initial-point)))]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :polygon
                               :attrs attrs}))))

(rf/dispatch [::action.events/register-action
              {:id :tool/polygon
               :label [::label "Polygon"]
               :icon "polygon-tool"
               :event [::tool.events/activate ::polygon]
               :active [::tool.subs/active? ::polygon]}])
