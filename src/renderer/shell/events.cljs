(ns renderer.shell.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events :refer [persist]]
   [renderer.shell.effects :as-alias shell.effects]))

(rf/reg-event-fx
 ::focus
 (fn [_ _]
   {::shell.effects/focus nil}))

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} _]
   (let [active-language (get-in db [:shell :active-language])]
     {:db (assoc-in db [:shell :language-status] {active-language :loading})
      ::shell.effects/init nil
      ::shell.effects/init-language [active-language
                                     {:on-success [::language-load-success]
                                      :on-error [::language-load-error]}]})))

(rf/reg-event-fx
 ::language-load-success
 [persist]
 (fn [{:keys [db]} [_ language]]
   {:db (assoc-in db [:shell :language-status language] :success)
    ::shell.effects/welcome language}))

(rf/reg-event-fx
 ::language-load-error
 [persist]
 (fn [{:keys [db]} [_ language error]]
   {:db (-> db
            (assoc-in [:shell :language-status language] :error)
            (assoc-in [:shell :active-language] :cljs))
    :dispatch [::app.events/toast-error error]}))

(rf/reg-event-fx
 ::activate-language
 (fn [{:keys [db]} [_ language]]
   (let [status (get-in db [:shell :language-status language])]
     (cond-> {:db (assoc-in db [:shell :active-language] language)}
       (not= status :success)
       (assoc ::shell.effects/init-language
              [language
               {:on-success [::language-load-success]
                :on-error [::language-load-error]}])))))
