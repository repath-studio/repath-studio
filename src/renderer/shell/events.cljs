(ns renderer.shell.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events :refer [persist]]
   [renderer.shell.effects :as-alias shell.effects]
   [renderer.shell.handlers :as shell.handlers]))

(rf/reg-event-fx
 ::focus
 (fn [_ _]
   {::shell.effects/focus nil}))

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} _]
   (let [active-language (shell.handlers/active-language db)]
     {:db (-> db
              (shell.handlers/reset-language-statuses)
              (shell.handlers/set-language-status active-language :loading))
      ::shell.effects/init nil
      ::shell.effects/init-language [active-language
                                     {:on-success [::language-load-success]
                                      :on-error [::language-load-error]}]})))

(rf/reg-event-fx
 ::language-load-success
 [persist]
 (fn [{:keys [db]} [_ language]]
   {:db (shell.handlers/set-language-status db language :success)
    ::shell.effects/welcome language}))

(rf/reg-event-fx
 ::language-load-error
 [persist]
 (fn [{:keys [db]} [_ language error]]
   {:db (-> db
            (shell.handlers/set-language-status language :error)
            (shell.handlers/activate-language :cljs))
    :dispatch [::app.events/toast-error error]}))

(rf/reg-event-fx
 ::activate-language
 (fn [{:keys [db]} [_ language]]
   (let [status (shell.handlers/language-status db language)]
     (cond-> {:db (shell.handlers/activate-language db language)}
       (not= status :success)
       (assoc ::shell.effects/init-language
              [language
               {:on-success [::language-load-success]
                :on-error [::language-load-error]}])))))
