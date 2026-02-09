(ns renderer.icon.events
  (:require
   [re-frame.core :as rf]
   [renderer.icon.handlers :as icon.handlers]))

(rf/reg-event-db
 ::register-icon
 (fn [db [_ icon]]
   (icon.handlers/register-icon db icon)))

(rf/reg-event-db
 ::deregister-icon
 (fn [db [_ id]]
   (icon.handlers/deregister-icon db id)))
