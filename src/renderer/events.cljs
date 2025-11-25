(ns renderer.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.handlers :as app.handlers]
   [renderer.effects :as-alias effects]))

(rf/reg-event-fx
 ::scroll-into-view
 (fn [_ [_ dom-el]]
   {::effects/scroll-into-view dom-el}))

(rf/reg-event-fx
 ::scroll-to-bottom
 (fn [_ [_ dom-el]]
   {::effects/scroll-to-bottom dom-el}))

(rf/reg-event-fx
 ::file-open
 (fn [_ [_ options]]
   {::effects/file-open options}))

(rf/reg-event-fx
 ::file-save
 (fn [_ [_ options]]
   {::effects/file-save options}))

(rf/reg-event-fx
 ::open-remote-url
 (fn [{:keys [db]} [_ url]]
   (if (app.handlers/desktop? db)
     {::effects/ipc-send ["open-remote-url" url]}
     {::effects/open-remote-url url})))
