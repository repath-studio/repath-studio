(ns renderer.tool.impl.element.polygon
  "https://www.w3.org/TR/SVG/shapes.html#PolygonElement"
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.element.poly :as poly]
   [renderer.tool.subs :as-alias tool.subs]))

(tool.hierarchy/derive-tool :polygon ::tool.hierarchy/poly)

(defmethod tool.hierarchy/on-double-click :polygon
  [db e]
  (-> db
      (poly/drop-last-point)
      (history.handlers/finalize (:timestamp e)
                                 [::create-polygon "Create polygon"])
      (tool.handlers/activate :transform)))

(rf/dispatch [::action.events/register-action
              {:id :tool/polygon
               :label [::label "Polygon"]
               :icon "polygon-tool"
               :event [::tool.events/activate :polygon]
               :active [::tool.subs/active? :polygon]}])
