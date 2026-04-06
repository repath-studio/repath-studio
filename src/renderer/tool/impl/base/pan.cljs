(ns renderer.tool.impl.base.pan
  (:require
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.app.effects :as-alias app.effects]
   [renderer.app.handlers :as app.handlers]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.key :as utils.key]))

(tool.hierarchy/derive-tool :pan ::tool.hierarchy/tool)

(defmethod tool.hierarchy/on-activate :pan
  [db]
  (tool.handlers/set-cursor db "grab"))

(defmethod tool.hierarchy/help [:pan :idle]
  []
  (i18n.views/t [::idle-help "Click and drag to pan."]))

(defmethod tool.hierarchy/on-pointer-up :pan
  [db _e]
  (tool.handlers/set-cursor db "grab"))

(defmethod tool.hierarchy/on-pointer-down :pan
  [db _e]
  (tool.handlers/set-cursor db "grabbing"))

(defmethod tool.hierarchy/on-drag :pan
  [db e]
  (frame.handlers/pan-by db (matrix/sub (:pointer-pos db)
                                        (:pointer-pos e))))

(defmethod tool.hierarchy/on-drag-end :pan
  [db _e]
  (-> db
      (tool.handlers/set-cursor "grab")
      (snap.handlers/update-viewport-tree)
      (app.handlers/add-fx [::app.effects/persist])))

(rf/dispatch [::action.events/register-action
              {:id :tool/pan
               :label [::tool-pan "Pan"]
               :icon "hand"
               :event [::tool.events/activate :pan]
               :active [::tool.subs/active? :pan]
               :shortcuts [{:keyCode (utils.key/codes "P")}]}])
