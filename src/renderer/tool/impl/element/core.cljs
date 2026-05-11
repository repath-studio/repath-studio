(ns renderer.tool.impl.element.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.element.handlers :as element.handlers]
   [renderer.hierarchy :as hierarchy]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.element.circle]
   [renderer.tool.impl.element.ellipse]
   [renderer.tool.impl.element.image]
   [renderer.tool.impl.element.line]
   [renderer.tool.impl.element.path]
   [renderer.tool.impl.element.poly]
   [renderer.tool.impl.element.polygon]
   [renderer.tool.impl.element.polyline]
   [renderer.tool.impl.element.rect]
   [renderer.tool.impl.element.svg]
   [renderer.tool.impl.element.text]))

(hierarchy/derive! ::tool.hierarchy/element ::tool.hierarchy/tool)

(defmethod tool.hierarchy/help [::tool.hierarchy/element :idle]
  []
  (i18n.views/t [::click-and-drag "Click and drag to create an element."]))

(defmethod tool.hierarchy/help [::tool.hierarchy/element :create]
  []
  (i18n.views/t [::release-to-finalize "Release to finalize the element."]))

(defmethod tool.hierarchy/on-activate ::tool.hierarchy/element
  [db]
  (tool.handlers/set-cursor db "crosshair"))

(defn snap-point
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label [::edge "edge"]})])

(defmethod tool.hierarchy/snapping-points [::tool.hierarchy/element :idle]
  [db]
  (snap-point db))

(defmethod tool.hierarchy/snapping-points [::tool.hierarchy/element :create]
  [db]
  (snap-point db))

(defmethod tool.hierarchy/snapping-elements [::tool.hierarchy/element :idle]
  [db]
  (element.handlers/visible db))

(defmethod tool.hierarchy/snapping-elements [::tool.hierarchy/element :create]
  [db]
  (element.handlers/visible db))

(rf/dispatch [::action.events/register-action-group
              {:id :tools/elements
               :label [::elements "Elements"]
               :actions [:tool/circle
                         :tool/ellipse
                         :tool/rect
                         :tool/line
                         :tool/path

                         :tool/polyline
                         :tool/polygon
                         :tool/image
                         :tool/text]}])

(rf/dispatch [::action.events/register-action-group
              {:id :tools/containers
               :label [::containers "Containers"]
               :actions [:tool/svg]}])
