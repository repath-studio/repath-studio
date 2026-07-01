(ns renderer.reepl.events
  (:require
   [re-frame.core :as rf]
   [renderer.reepl.effects :as reepl.effects]))

(rf/reg-event-fx
 ::focus
 (fn [_ _]
   {::reepl.effects/focus nil}))
