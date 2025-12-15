(ns renderer.tool.impl.misc.dropper
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.effects]
   [renderer.app.events :as-alias app.events]
   [renderer.app.handlers :as app.handlers]
   [renderer.document.handlers :as document.handlers]
   [renderer.effects :as-alias effects]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(defmethod tool.hierarchy/properties :dropper
  []
  {:icon "eye-dropper"
   :label [::label "Dropper"]})

(defmethod tool.hierarchy/help [:dropper :idle]
  []
  (i18n.views/t [::help "Click anywhere to pick a color."]))

(defmethod tool.hierarchy/on-activate :dropper
  [db]
  (if (contains? (:features db) :eye-dropper)
    (app.handlers/add-fx db [::effects/eye-dropper {:on-success [::success]
                                                    :on-error [::error]}])
    (-> db
        (tool.handlers/activate :transform)
        (app.handlers/add-fx [::app.effects/toast
                              [:error ["Eye Dropper is not available in this
                                        environment."]]]))))

(rf/reg-event-db
 ::success
 (fn [db [_ ^js color]]
   (let [srgb-color (.-sRGBHex color)]
     (-> db
         (document.handlers/assoc-attr :fill srgb-color)
         (element.handlers/assoc-attr :fill srgb-color)
         (history.handlers/finalize [::pick-color "Pick color"])
         (tool.handlers/activate :transform)))))

(rf/reg-event-db
 ::error
 (fn [db [_ error]]
   (-> db
       (tool.handlers/activate :transform)
       (app.handlers/add-fx [:dispatch [::app.events/toast-error error]]))))
