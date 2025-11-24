(ns renderer.a11y.events
  (:require
   [re-frame.core :as rf]
   [renderer.a11y.handlers :as a11y.handlers]))

(rf/reg-event-db
 ::register-filter
 (fn [db [_ a11y-filter]]
   (a11y.handlers/register-filter db a11y-filter)))

(rf/reg-event-db
 ::deregister-filter
 (fn [db [_ id]]
   (a11y.handlers/deregister-filter db id)))

(rf/reg-event-db
 ::toggle-active-filter
 [(rf/path :a11y)]
 (fn [db [_ id]]
   (if (= (:active-filter db) id)
     (dissoc db :active-filter)
     (assoc db :active-filter id))))
