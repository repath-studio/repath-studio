(ns renderer.tool.impl.base.pan
  (:require
   [clojure.core.matrix :as matrix]
   [renderer.app.effects :as-alias app.effects]
   [renderer.app.handlers :as app.handlers]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(defmethod tool.hierarchy/properties :pan
  []
  {:icon "hand"
   :label [::label "Pan"]})

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
