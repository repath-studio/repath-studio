(ns renderer.i18n.events
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.app.events :refer [persist]]
   [renderer.effects :as-alias effects]
   [renderer.i18n.events :as-alias i18n.events]
   [renderer.i18n.handlers :as i18n.handlers]
   [renderer.i18n.subs :as-alias i18n.subs]))

(rf/reg-event-fx
 ::set-lang-attrs
 (fn [{:keys [db]} _]
   (let [{:keys [languages user-lang system-lang]} db
         lang (i18n.handlers/computed-lang languages user-lang system-lang)
         dir (get-in languages [lang :dir])]
     {:fx [[::effects/set-document-attr ["lang" lang]]
           [::effects/set-document-attr ["dir" dir]]]})))

(rf/reg-event-fx
 ::set-user-lang
 [persist]
 (fn [{:keys [db]} [_ lang]]
   {:db (assoc db :user-lang lang)
    :dispatch [::set-lang-attrs]}))

(defn action-id
  [id]
  (keyword :lang id))

(rf/reg-event-fx
 ::register-language
 (fn [{:keys [db]} [_ language]]
   (let [{:keys [id locale]} language]
     {:db (i18n.handlers/register-language db language)
      :dispatch-n [[::action.events/register-action
                    {:id (action-id id)
                     :label [(action-id id) locale]
                     :icon "language"
                     :event [::i18n.events/set-user-lang id]
                     :active [::i18n.subs/selected-lang? id]}]
                   [::action.events/add-action-to-group
                    :i18n/language
                    (action-id id)]]})))

(rf/reg-event-fx
 ::deregister-language
 (fn [{:keys [db]} [_ id]]
   {:db (i18n.handlers/deregister-language db id)
    :dispatch-n [[::action.events/remove-action-from-group
                  :i18n/language
                  (action-id id)]
                 [::action.events/deregister-action (action-id id)]]}))

(rf/reg-event-db
 ::set-translation
 (fn [db [_ lang-id k v]]
   (i18n.handlers/set-translation db lang-id k v)))
