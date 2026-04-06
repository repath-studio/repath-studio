(ns renderer.tool.impl.misc.fill
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.element :as utils.element]
   [renderer.utils.key :as utils.key]))

(tool.hierarchy/derive-tool :fill ::tool.hierarchy/tool)

(defmethod tool.hierarchy/help [:fill :idle]
  []
  (i18n.views/t [::help "Click on an element to fill."]))

(defmethod tool.hierarchy/on-activate :fill
  [db]
  (tool.handlers/set-cursor db "crosshair"))

(defn fill
  [db]
  (let [color (document.handlers/attr db :fill)
        el-id (-> db :clicked-element :id)]
    (-> db
        (dissoc :clicked-element)
        (element.handlers/set-attr el-id :fill color)
        (history.handlers/finalize [::fill "Fill"]))))

(defmethod tool.hierarchy/on-pointer-up :fill
  [db _e]
  (fill db))

(defmethod tool.hierarchy/on-drag-end :fill
  [db _e]
  (fill db))

(defmethod tool.hierarchy/on-pointer-move :fill
  [db e]
  (let [color (document.handlers/attr db :fill)
        el (-> e :element)]
    (cond-> (history.handlers/reset-state db)
      (not (utils.element/root? el))
      (element.handlers/set-attr (:id el) :fill color))))

(defmethod tool.hierarchy/on-pointer-down :fill
  [db e]
  (-> db
      (assoc :clicked-element (-> e :element))
      (history.handlers/reset-state)))

(rf/dispatch [::action.events/register-action
              {:id :tool/fill
               :label [::label "Fill"]
               :icon "fill"
               :event [::tool.events/activate :fill]
               :active [::tool.subs/active? :fill]
               :shortcuts [{:keyCode (utils.key/codes "F")}]}])
