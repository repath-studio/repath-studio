(ns renderer.tool.impl.draw.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.hierarchy :as hierarchy]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.draw.brush]
   [renderer.tool.impl.draw.pencil]))

(hierarchy/derive! ::tool.hierarchy/draw ::tool.hierarchy/tool)

(defmethod tool.hierarchy/help [::tool.hierarchy/draw :idle]
  []
  (i18n.views/t [::click-and-drag "Click and drag to draw."]))

(defmethod tool.hierarchy/help [::tool.hierarchy/draw :create]
  []
  (i18n.views/t [::release-to-finalize "Release to finalize the drawing."]))

(defmethod tool.hierarchy/on-activate ::tool.hierarchy/draw
  [db]
  (tool.handlers/set-cursor db "crosshair"))

(rf/dispatch [::action.events/register-action-group
              {:id :tools/draw
               :label [::draw "Draw"]
               :actions [:tool/pencil
                         :tool/brush]}])
