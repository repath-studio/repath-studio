(ns renderer.tool.impl.draw.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.draw.brush]
   [renderer.tool.impl.draw.pencil]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.key :as utils.key]))

(tool.hierarchy/derive-tool ::tool.hierarchy/draw ::tool.hierarchy/tool)

(defmethod tool.hierarchy/help [::tool.hierarchy/draw :idle]
  []
  (i18n.views/t [::help "Click and drag to draw."]))

(defmethod tool.hierarchy/on-activate ::tool.hierarchy/draw
  [db]
  (tool.handlers/set-cursor db "crosshair"))

(rf/dispatch [::action.events/register-action
              {:id :tool/pencil
               :label [::label "Pen"]
               :icon "pencil"
               :event [::tool.events/activate :pencil]
               :active [::tool.subs/active? :pencil]}])

(rf/dispatch [::action.events/register-action
              {:id :tool/brush
               :label [::label "Brush"]
               :icon "brush"
               :event [::tool.events/activate :brush]
               :active [::tool.subs/active? :brush]
               :shortcuts [{:keyCode (utils.key/codes "B")}]}])

(rf/dispatch [::action.events/register-action-group
              {:id :tools/draw
               :label [::draw "Draw"]
               :actions [:tool/pencil
                         :tool/brush]}])
