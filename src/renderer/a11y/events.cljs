(ns renderer.a11y.events
  (:require
   [re-frame.core :as rf]
   [renderer.a11y.handlers :as a11y.handlers]
   [renderer.a11y.subs :as-alias a11y.subs]
   [renderer.action.events :as-alias action.events]))

(rf/reg-event-db
 ::toggle-active-filter
 [(rf/path :a11y)]
 (fn [db [_ id]]
   (if (= (:active-filter db) id)
     (dissoc db :active-filter)
     (assoc db :active-filter id))))

(defn action-id
  [id]
  (keyword :filter id))

(rf/reg-event-fx
 ::register-filter
 (fn [{:keys [db]} [_ a11y-filter]]
   (let [{:keys [id label]} a11y-filter]
     {:db (a11y.handlers/register-filter db a11y-filter)
      :dispatch-n [[::action.events/register-action
                    {:id (action-id id)
                     :label label
                     :icon "a11y"
                     :active [::a11y.subs/filter-active? id]
                     :event [::toggle-active-filter id]}]
                   [::action.events/add-action-to-group
                    :a11y/filter
                    (action-id id)]]})))

(rf/reg-event-fx
 ::deregister-filter
 (fn [{:keys [db]} [_ id]]
   {:db (a11y.handlers/deregister-filter db id)
    :dispatch-n [[::action.events/remove-action-from-group
                  :a11y/filter
                  (action-id id)]
                 [::action.events/deregister-action (action-id id)]]}))
