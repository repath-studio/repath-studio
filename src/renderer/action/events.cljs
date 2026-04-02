(ns renderer.action.events
  (:require
   [re-frame.core :as rf]
   [re-pressed.core :as re-pressed]
   [renderer.action.handlers :as action.handlers]
   [renderer.utils.key :as utils.key]))

(rf/reg-event-fx
 ::register-action
 (fn [{:keys [db]} [_ action]]
   {:db (action.handlers/register db action)
    :dispatch [::re-pressed/set-keydown-rules
               (-> (action.handlers/entities db)
                   (utils.key/actions->keydown-rules))]}))

(rf/reg-event-fx
 ::deregister-action
 (fn [db [_ action-id]]
   {:db (action.handlers/deregister db action-id)
    :dispatch [::re-pressed/set-keydown-rules
               (-> (action.handlers/entities db)
                   (utils.key/actions->keydown-rules))]}))
