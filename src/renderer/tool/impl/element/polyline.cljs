(ns renderer.tool.impl.element.polyline
  "https://www.w3.org/TR/SVG/shapes.html#PolylineElement"
  (:require
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.element.poly :as poly]))

(tool.hierarchy/derive-tool :polyline ::tool.hierarchy/poly)

(defmethod tool.hierarchy/properties :polyline
  []
  {:icon "polyline"
   :label [::label "Polyline"]})

(defmethod tool.hierarchy/on-double-click :polyline
  [db _e]
  (-> db
      (poly/drop-last-point)
      (history.handlers/finalize [::create-polyline "Create polyline"])
      (tool.handlers/activate :transform)))
