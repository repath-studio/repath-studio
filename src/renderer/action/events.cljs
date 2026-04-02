(ns renderer.action.events
  (:require
   [re-frame.core :as rf]
   [renderer.action.effects :as-alias action.effects]
   [renderer.action.handlers :as action.handlers]))

(rf/reg-event-fx
 ::register-action
 (fn [{:keys [db]} [_ action]]
   {:db (action.handlers/register db action)
    ::action.effects/update-keydown-rules (action.handlers/entities db)}))

(rf/reg-event-fx
 ::deregister-action
 (fn [{:keys [db]} [_ id]]
   {:db (action.handlers/deregister db id)
    ::action.effects/update-keydown-rules (action.handlers/entities db)}))
