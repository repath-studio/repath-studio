(ns renderer.shell.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events :refer [persist]]
   [renderer.shell.db :as shell.db]
   [renderer.shell.effects :as-alias shell.effects]
   [renderer.shell.handlers :as shell.handlers]
   [replumb.repl :as repl]))

(rf/reg-event-fx
 ::focus
 (fn [_ _]
   {::shell.effects/focus nil}))

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} _]
   (let [lang (shell.handlers/active-language db)]
     {:db (-> db
              (shell.handlers/reset-language-statuses)
              (update-in [:shell :languages lang]
                         #(merge shell.db/default-lang %))
              (shell.handlers/set-language-status :loading))
      ::shell.effects/init [[::add-item]
                            {:language lang
                             :on-success [::language-load-success]
                             :on-error [::language-load-error]}]})))

(rf/reg-event-fx
 ::language-load-success
 [persist]
 (fn [{:keys [db]} _]
   (let [lang (shell.handlers/active-language db)]
     (cond-> {:db (shell.handlers/set-language-status db :success)}
       (empty? (get-in db [:shell :languages lang :items]))
       (assoc ::shell.effects/welcome lang)))))

(rf/reg-event-fx
 ::language-load-error
 [persist]
 (fn [{:keys [db]} [_ error]]
   {:db (-> db
            (shell.handlers/set-language-status :error)
            (shell.handlers/set-language :cljs))
    :dispatch [::app.events/toast-error error]}))

(rf/reg-event-fx
 ::activate-language
 [persist]
 (fn [{:keys [db]} [_ lang]]
   (let [status (shell.handlers/language-status db lang)]
     (cond-> {:db (shell.handlers/set-language db lang)}
       (not status)
       (update-in [:db :shell :languages lang] #(merge shell.db/default-lang %))

       (not= status :success)
       (assoc ::shell.effects/init-language
              {:language lang
               :on-success [::language-load-success]
               :on-error [::language-load-error]})))))

(rf/reg-event-db
 ::clear-items
 [persist]
 (fn [db _]
   (shell.handlers/clear-items db)))

(rf/reg-event-db
 ::add-item
 [persist]
 (fn [db [_ item-type value]]
   (shell.handlers/add-item db {:type item-type
                                :value value})))

(rf/reg-event-db
 ::go-up
 [persist]
 (fn [db _]
   (shell.handlers/update-history-position db inc)))

(rf/reg-event-db
 ::go-down
 [persist]
 (fn [db _]
   (shell.handlers/update-history-position db dec)))

(rf/reg-event-db
 ::set-text
 [persist]
 (fn [db [_ text]]
   (shell.handlers/set-text db text)))

(rf/reg-event-fx
 ::execute
 [persist]
 (fn [{:keys [db]} [_ text]]
   {:db (-> db
            (shell.handlers/update-history-position dec)
            (shell.handlers/set-text text)
            (shell.handlers/reset-history-position)
            (shell.handlers/add-to-history "")
            (shell.handlers/add-item {:type :input
                                      :value {:text text
                                              :current-ns (repl/current-ns)
                                              :num (-> db
                                                       shell.handlers/history
                                                       count)}}))
    ::shell.effects/execute {:text text
                             :language (shell.handlers/active-language db)
                             :verbose (shell.handlers/verbose? db)
                             :event [::add-item]}}))
