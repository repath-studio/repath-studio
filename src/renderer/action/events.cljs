(ns renderer.action.events
  (:require
   [re-frame.core :as rf]
   [renderer.action.handlers :as action.handlers]))

(rf/reg-event-db
 ::register-action
 (fn [db [_ action]]
   (action.handlers/register db action)))

(rf/reg-event-db
 ::deregister-action
 (fn [db [_ action-id]]
   (action.handlers/deregister db action-id)))
