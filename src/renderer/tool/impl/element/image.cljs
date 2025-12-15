(ns renderer.tool.impl.element.image
  "https://www.w3.org/TR/SVG/embedded.html#ImageElement"
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.app.handlers :as app.handlers]
   [renderer.effects :as-alias effects]
   [renderer.element.db :as element.db]
   [renderer.element.effects :as-alias element.effects]
   [renderer.element.events :as-alias element.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(defmethod tool.hierarchy/properties :image
  []
  {:icon "image"
   :label [::label "Image"]})

(defmethod tool.hierarchy/on-drag-end :image
  [db e]
  (tool.hierarchy/on-pointer-up db e))

(defmethod tool.hierarchy/on-pointer-up :image
  [db _e]
  (app.handlers/add-fx
   db
   [::effects/file-open
    {:options {:startIn "pictures"
               :id "image-picker"
               :types [{:accept element.db/image-mime-types}]}
     :on-success [::success]
     :on-error [::app.events/toast-error]}]))

(rf/reg-event-fx
 ::success
 (fn [{:keys [db]} [_ _file-handle file]]
   {:db (tool.handlers/activate db :transform)
    ::element.effects/import-image
    {:file file
     :on-success [::element.events/add]
     :on-error [::app.events/toast-error]
     :position (or (:point (:nearest-neighbor db))
                   (:adjusted-pointer-offset db))}}))
