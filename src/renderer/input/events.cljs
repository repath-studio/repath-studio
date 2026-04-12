(ns renderer.input.events
  (:require
   [re-frame.core :as rf]
   [renderer.input.handlers :as input.handlers]))

(rf/reg-event-db
 ::pointer
 (fn [db [_ e]]
   (input.handlers/pointer db e)))

(rf/reg-event-db
 ::wheel
 (fn [db [_ e]]
   (input.handlers/wheel db e)))

(rf/reg-event-db
 ::drag
 (fn [db [_ e]]
   (input.handlers/drag db e)))

(rf/reg-event-db
 ::keyboard
 (fn [db [_ e]]
   (input.handlers/keyboard db e)))
