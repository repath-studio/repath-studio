(ns renderer.timeline.events
  (:require
   [re-frame.core :as rf]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.events :refer [finalize]]
   [renderer.timeline.effects :as-alias timeline.effects]
   [renderer.utils.attribute :as utils.attribute]))

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
 ::toggle-auto-duration
 (fn [db _]
   (update-in db [:timeline :auto-duration] not)))

(rf/reg-event-db
 ::set-speed
 (fn [db [_ speed]]
   (assoc-in db [:timeline :speed] speed)))

(rf/reg-event-db
 ::preview-action
 (fn [db [_ id start end]]
   (cond-> db
     :always
     (-> (element.handlers/toggle-selection id false)
         (element.handlers/set-attr :begin (utils.attribute/->fixed start))
         (element.handlers/set-attr :end (utils.attribute/->fixed end)))

     (-> db :timeline :auto-duration)
     (element.handlers/set-attr :dur (utils.attribute/->fixed (- end start))))))

(rf/reg-event-db
 ::finalize-action
 [(finalize [::update-animation "Update animation"])]
 (fn [db [_ start end]]
   (-> db
       (element.handlers/set-attr :begin (utils.attribute/->fixed start))
       (element.handlers/set-attr :end (utils.attribute/->fixed end)))))

(rf/reg-event-fx
 ::set-time
 (fn [{:keys [db]} [_ t]]
   {:db (assoc-in db [:timeline :time] t)
    ::timeline.effects/set-current-time t
    ::timeline.effects/pause-animations nil}))
