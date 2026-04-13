(ns renderer.tool.impl.element.polyline
  "https://www.w3.org/TR/SVG/shapes.html#PolylineElement"
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.element.poly :as poly]
   [renderer.tool.subs :as-alias tool.subs]))

(tool.hierarchy/derive-tool :polyline ::tool.hierarchy/poly)

(defmethod tool.hierarchy/on-double-click :polyline
  [db e]
  (-> db
      (poly/drop-last-point)
      (history.handlers/finalize (:timestamp e)
                                 [::create-polyline "Create polyline"])
      (tool.handlers/activate :transform)))

(rf/dispatch [::action.events/register-action
              {:id :tool/polyline
               :label [::label "Polyline"]
               :icon "polyline"
               :event [::tool.events/activate :polyline]
               :active [::tool.subs/active? :polyline]}])
