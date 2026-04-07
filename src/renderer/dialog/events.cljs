(ns renderer.dialog.events
  (:require
   [config :as config]
   [re-frame.core :as rf]
   [renderer.dialog.handlers :as dialog.handlers]
   [renderer.dialog.views :as dialog.views]
   [renderer.i18n.handlers :as i18n.handlers]))

(rf/reg-event-db
 ::show-cmdk
 (fn [db [_ title]]
   (dialog.handlers/create db {:title [:div.sr-only (i18n.handlers/t db title)]
                               :content [dialog.views/cmdk]
                               :attrs {:class "top-5 md:top-10 translate-y-0 p-0
                                               w-150"}})))

(rf/reg-event-db
 ::show-about
 (fn [db [_]]
   (dialog.handlers/create db {:title config/app-name
                               :content [dialog.views/about]})))

(rf/reg-event-db
 ::confirm-irreversible-action
 (fn [db [_ data]]
   (dialog.handlers/create
    db
    {:title (i18n.handlers/t db [::are-you-sure
                                 "Are you sure you want to continue?"])
     :content [dialog.views/confirmation data]})))

(rf/reg-event-fx
 ::close
 (fn [{:keys [db]} [_ on-close]]
   (cond-> {:db (update db :dialogs pop)}
     on-close
     (assoc :dispatch on-close))))
