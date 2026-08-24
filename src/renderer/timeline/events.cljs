(ns renderer.timeline.events
  (:require
   [re-frame.core :as rf]
   [renderer.history.events :refer [finalize]]
   [renderer.timeline.effects :as-alias timeline.effects]
   [renderer.timeline.handlers :as timeline.handlers]))

(rf/reg-event-db
 ::pause
 (fn [db _]
   (assoc-in db [:timeline :paused] true)))

(rf/reg-event-db
 ::play
 (fn [db _]
   (assoc-in db [:timeline :paused] false)))

(rf/reg-event-db
 ::toggle-grid-snap
 (fn [db _]
   (update-in db [:timeline :grid-snap] not)))

(rf/reg-event-db
 ::toggle-guide-snap
 (fn [db _]
   (update-in db [:timeline :guide-snap] not)))

(rf/reg-event-db
 ::toggle-replay
 (fn [db _]
   (update-in db [:timeline :replay] not)))

(rf/reg-event-db
 ::toggle-fit-duration
 (fn [db _]
   (update-in db [:timeline :fit-duration] not)))

(rf/reg-event-db
 ::set-speed
 (fn [db [_ speed]]
   (assoc-in db [:timeline :speed] speed)))

(rf/reg-event-db
 ::preview-action
 (fn [db [_ id start end]]
   (timeline.handlers/update-action db id start end)))

(rf/reg-event-db
 ::finalize-action
 [(finalize [::update-animation "Update animation"])]
 (fn [db [_ id start end]]
   (timeline.handlers/update-action db id start end)))

(rf/reg-event-fx
 ::set-time
 (fn [{:keys [db]} [_ t]]
   {:db (assoc-in db [:timeline :time] t)
    ::timeline.effects/set-current-time t
    ::timeline.effects/pause-animations nil}))
