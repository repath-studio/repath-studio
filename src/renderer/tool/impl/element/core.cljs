(ns renderer.tool.impl.element.core
  (:require
   [renderer.element.handlers :as element.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.element.circle]
   [renderer.tool.impl.element.ellipse]
   [renderer.tool.impl.element.image]
   [renderer.tool.impl.element.line]
   [renderer.tool.impl.element.poly]
   [renderer.tool.impl.element.polygon]
   [renderer.tool.impl.element.polyline]
   [renderer.tool.impl.element.rect]
   [renderer.tool.impl.element.svg]
   [renderer.tool.impl.element.text]))

(tool.hierarchy/derive-tool ::tool.hierarchy/element ::tool.hierarchy/tool)

(defmethod tool.hierarchy/help [::tool.hierarchy/element :idle]
  []
  (i18n.views/t [::help "Click and drag to create an element."]))

(defmethod tool.hierarchy/on-activate ::tool.hierarchy/element
  [db]
  (tool.handlers/set-cursor db "crosshair"))

(defmethod tool.hierarchy/snapping-points ::tool.hierarchy/element
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label [::edge "edge"]})])

(defmethod tool.hierarchy/snapping-elements ::tool.hierarchy/element
  [db]
  (element.handlers/visible db))
