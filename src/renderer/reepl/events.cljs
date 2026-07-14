(ns renderer.reepl.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events :refer [persist]]
   [renderer.reepl.effects :as reepl.effects]))

(rf/reg-event-fx
 ::focus
 (fn [_ _]
   {::reepl.effects/focus nil}))

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} _]
   (let [active-language (get-in db [:shell :active-language])]
     {:db (assoc-in db [:shell :language-status] {active-language :loading})
      ::reepl.effects/init nil
      ::reepl.effects/init-language active-language})))

(rf/reg-event-db
 ::language-load-success
 [persist]
 (fn [db _]
   (let [active-language (get-in db [:shell :active-language])]
     (assoc-in db [:shell :language-status active-language] :success))))

(rf/reg-event-fx
 ::language-load-error
 [persist]
 (fn [{:keys [db]} [_ error]]
   (let [active-language (get-in db [:shell :active-language])]
     {:db (-> db
              (assoc-in [:shell :language-status active-language] :error)
              (assoc-in [:shell :active-language] :cljs))
      :dispatch [::app.events/toast-error error]})))

(rf/reg-event-fx
 ::activate-language
 (fn [{:keys [db]} [_ language]]
   (let [status (get-in db [:shell :language-status language])]
     (cond-> {:db (assoc-in db [:shell :active-language] language)}
       (not= status :success)
       (assoc ::reepl.effects/init-language language)))))
