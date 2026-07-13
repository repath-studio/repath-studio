(ns renderer.reepl.events
  (:require
   [re-frame.core :as rf]
   [renderer.reepl.effects :as reepl.effects]))

(rf/reg-event-fx
 ::focus
 (fn [_ _]
   {::reepl.effects/focus nil}))

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:shell :language-state] :loading)
    ::reepl.effects/init (:repl-mode db)}))

(rf/reg-event-db
 ::language-loaded
 (fn [db _]
   (assoc-in db [:shell :language-state] :loaded)))
