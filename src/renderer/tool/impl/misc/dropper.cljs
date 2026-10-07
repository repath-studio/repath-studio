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
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.platform :as utils.platform]))

(hierarchy/derive! ::eye-dropper ::tool.hierarchy/tool)

(defmethod tool.hierarchy/help [::eye-dropper :idle]
  []
  (i18n.views/t [::help "Click anywhere to pick a color."]))

(defmethod tool.hierarchy/on-activate ::eye-dropper
  [db]
  (if (contains? (:features db) :eye-dropper)
    (app.handlers/enqueue-fx db [::effects/eye-dropper {:on-success [::success]
                                                        :on-error [::error]}])
    (-> db
        (tool.handlers/deactivate)
        (app.handlers/enqueue-fx [::app.effects/toast
                                  [:error ["Eye Dropper is not available in this
                                        environment."]]]))))

(rf/reg-event-fx
 ::success
 [(rf/inject-cofx ::effects/now)]
 (fn [{:keys [db now]} [_ ^js color]]
   {:db (let [srgb-color (.-sRGBHex color)]
          (-> db
              (document.handlers/assoc-attr :fill srgb-color)
              (element.handlers/assoc-attr :fill srgb-color)
              (history.handlers/finalize now [::pick-color "Pick color"])
              (tool.handlers/deactivate)))}))

(rf/reg-event-db
 ::error
 (fn [db [_ error]]
   (cond-> db
     :always
     (tool.handlers/deactivate)

     (or (not= (.-name error) "AbortError")
         ;; EyeDropper is not working properly on Linux Wayland, but returns an
         ;; AbortError. We show the error on Linux to avoid failing silently.
         (utils.platform/linux? (:platform db)))
     (app.handlers/enqueue-fx [:dispatch [::app.events/toast-error error]]))))

(rf/dispatch [::action.events/register-action
              {:id :tool/eye-dropper
               :label [::label "Eyedropper"]
               :icon "eye-dropper"
               :event [::tool.events/activate ::eye-dropper]
               :active [::tool.subs/active? ::eye-dropper]
               :available [::app.subs/supported-feature? :eye-dropper]}])
