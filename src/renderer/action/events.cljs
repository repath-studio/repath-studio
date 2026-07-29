(ns renderer.action.events
  (:require
   [re-frame.core :as rf]
   [renderer.action.effects :as-alias action.effects]
   [renderer.action.handlers :as action.handlers]
   [renderer.app.events :refer [persist]]))

(rf/reg-event-fx
 ::register-action
 (fn [{:keys [db]} [_ action]]
   (let [db (action.handlers/register-action db action)]
     {:db db
      ::action.effects/update-keydown-rules
      (action.handlers/actions-with-shortcuts db)})))

(rf/reg-event-fx
 ::deregister-action
 (fn [{:keys [db]} [_ id]]
   (let [db (action.handlers/deregister-action db id)]
     {:db db
      ::action.effects/update-keydown-rules
      (action.handlers/actions-with-shortcuts db)})))

(rf/reg-event-fx
 ::add-shortcut
 [persist]
 (fn [{:keys [db]} [_ id shortcut]]
   (let [db (action.handlers/add-shortcut db id shortcut)]
     {:db db
      ::action.effects/update-keydown-rules
      (action.handlers/actions-with-shortcuts db)})))

(rf/reg-event-fx
 ::remove-shortcut
 [persist]
 (fn [{:keys [db]} [_ id shortcut]]
   (let [db (action.handlers/remove-shortcut db id shortcut)]
     {:db db
      ::action.effects/update-keydown-rules
      (action.handlers/actions-with-shortcuts db)})))

(rf/reg-event-fx
 ::reset-shortcuts
 [persist]
 (fn [{:keys [db]} [_ id]]
   (let [db (action.handlers/reset-shortcuts db id)]
     {:db db
      ::action.effects/update-keydown-rules
      (action.handlers/actions-with-shortcuts db)})))

(rf/reg-event-db
 ::register-action-group
 (fn [db [_ group]]
   (action.handlers/register-action-group db group)))

(rf/reg-event-db
 ::deregister-action-group
 (fn [db [_ id]]
   (action.handlers/deregister-action-group db id)))

(rf/reg-event-db
 ::add-action-to-group
 (fn [db [_ group-id action-id]]
   (action.handlers/add-action-to-group db group-id action-id)))

(rf/reg-event-db
 ::remove-action-from-group
 (fn [db [_ group-id action-id]]
   (action.handlers/remove-action-from-group db group-id action-id)))
