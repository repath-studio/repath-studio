(ns renderer.tool.impl.misc.dropper
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.app.effects :as-alias app.effects]
   [renderer.app.events :as-alias app.events]
   [renderer.app.handlers :as app.handlers]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.handlers :as document.handlers]
   [renderer.effects :as-alias effects]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.key :as utils.key]))

(tool.hierarchy/derive-tool :eye-dropper ::tool.hierarchy/tool)

(defmethod tool.hierarchy/help [:eye-dropper :idle]
  []
  (i18n.views/t [::help "Click anywhere to pick a color."]))

(defmethod tool.hierarchy/on-activate :eye-dropper
  [db]
  (if (contains? (:features db) :eye-dropper)
    (app.handlers/add-fx db [::effects/eye-dropper {:on-success [::success]
                                                    :on-error [::error]}])
    (-> db
        (tool.handlers/activate :transform)
        (app.handlers/add-fx [::app.effects/toast
                              [:error ["Eye Dropper is not available in this
                                        environment."]]]))))

(rf/reg-event-fx
 ::success
 (fn [{:keys [db now]} [_ ^js color]]
   {:db (let [srgb-color (.-sRGBHex color)]
          (-> db
              (document.handlers/assoc-attr :fill srgb-color)
              (element.handlers/assoc-attr :fill srgb-color)
              (history.handlers/finalize now [::pick-color "Pick color"])
              (tool.handlers/activate :transform)))}))

(rf/reg-event-db
 ::error
 (fn [db [_ error]]
   (-> db
       (tool.handlers/activate :transform)
       (app.handlers/add-fx [:dispatch [::app.events/toast-error error]]))))

(rf/dispatch [::action.events/register-action
              {:id :tool/eye-dropper
               :label [::label "Eyedropper"]
               :icon "eye-dropper"
               :event [::tool.events/activate :eye-dropper]
               :active [::tool.subs/active? :eye-dropper]
               :available [::app.subs/supported-feature? :eye-dropper]
               :shortcuts [{:keyCode (utils.key/codes "D")}]}])
