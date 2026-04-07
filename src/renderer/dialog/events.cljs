(ns renderer.dialog.events
  (:require
   [config :as config]
   [re-frame.core :as rf]
   [renderer.dialog.handlers :as dialog.handlers]
   [renderer.dialog.views :as dialog.views]))

(rf/reg-event-db
 ::show-cmdk
 (fn [db [_]]
   (dialog.handlers/create db {:title [:div.sr-only "Command panel"]
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
   (dialog.handlers/create db {:title (dialog.views/irreversible-title)
                               :content [dialog.views/confirmation data]})))

(rf/reg-event-fx
 ::close
 (fn [{:keys [db]} [_ on-close]]
   (cond-> {:db (update db :dialogs pop)}
     on-close
     (assoc :dispatch on-close))))
