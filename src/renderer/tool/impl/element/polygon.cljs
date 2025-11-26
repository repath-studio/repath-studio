(ns renderer.tool.impl.element.polygon
  "https://www.w3.org/TR/SVG/shapes.html#PolygonElement"
  (:require
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.element.poly :as poly]))

(derive :polygon ::tool.hierarchy/poly)

(defmethod tool.hierarchy/properties :polygon
  []
  {:icon "polygon-tool"
   :label [::label "Polygon"]})

(defmethod tool.hierarchy/on-double-click :polygon
  [db _e]
  (-> db
      (poly/drop-last-point)
      (history.handlers/finalize [::create-polygon "Create polygon"])
      (tool.handlers/activate :transform)))
