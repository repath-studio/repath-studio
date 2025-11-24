(ns renderer.a11y.events
  (:require
   [re-frame.core :as rf]
   [renderer.a11y.handlers :as a11y.handlers]
   [renderer.app.events :refer [persist]]))

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
 [persist]
 (fn [db [_ id]]
   (if (= (-> db :a11y :active-filter) id)
     (update db :a11y dissoc :active-filter)
     (assoc-in db [:a11y :active-filter] id))))
