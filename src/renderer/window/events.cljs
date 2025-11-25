(ns renderer.window.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.effects]
   [renderer.app.handlers :as app.handlers]
   [renderer.effects :as-alias effects]
   [renderer.menubar.handlers :as menubar.handlers]
   [renderer.window.effects :as-alias window.effects]))

(rf/reg-event-db
 ::set-maximized
 (fn [db [_ state]]
   (assoc-in db [:window :maximized] state)))

(rf/reg-event-db
 ::set-fullscreen
 (fn [db [_ state]]
   (assoc-in db [:window :fullscreen] state)))

(rf/reg-event-db
 ::set-minimized
 (fn [db [_ state]]
   (assoc-in db [:window :minimized] state)))

(rf/reg-event-db
 ::set-focused
 (fn [db [_ state]]
   (assoc-in db [:window :focused] state)))

(rf/reg-event-fx
 ::update-focused
 [(rf/inject-cofx ::window.effects/focused)]
 (fn [{:keys [db focused]} _]
   {:db (cond-> db
          :always
          (assoc-in [:window :focused] focused)

          (not focused)
          (menubar.handlers/deactivate))}))

(rf/reg-event-fx
 ::update-width
 [(rf/inject-cofx ::window.effects/width)]
 (fn [{:keys [db width]} _]
   {:db (assoc-in db [:window :width] width)}))

(rf/reg-event-fx
 ::update-fullscreen
 [(rf/inject-cofx ::window.effects/fullscreen)]
 (fn [{:keys [db fullscreen]} _]
   {:db (assoc-in db [:window :fullscreen] fullscreen)}))

(rf/reg-event-fx
 ::close
 (fn [_ _]
   {::window.effects/close nil}))

(rf/reg-event-fx
 ::relaunch
 (fn [{:keys [db]} _]
   (if (app.handlers/desktop? db)
     {::effects/ipc-send ["relaunch"]}
     {::window.effects/reload nil})))

(rf/reg-event-fx
 ::clear-data-and-relaunch
 (fn [_ _]
   {:fx [[::app.effects/clear-local-store nil]
         [:dispatch [::relaunch]]]}))

(rf/reg-event-fx
 ::toggle-maximized
 (fn [_ _]
   {::effects/ipc-send ["window-toggle-maximized"]}))

(rf/reg-event-fx
 ::toggle-fullscreen
 (fn [{:keys [db]} _]
   (if (app.handlers/desktop? db)
     {::effects/ipc-send ["window-toggle-fullscreen"]}
     {::window.effects/toggle-fullscreen nil})))

(rf/reg-event-fx
 ::minimize
 (fn [_ _]
   {::effects/ipc-send ["window-minimize"]}))
