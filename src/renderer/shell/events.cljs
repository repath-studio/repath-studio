(ns renderer.shell.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.effects]
   [renderer.app.events :as-alias app.events :refer [persist]]
   [renderer.shell.db :as shell.db]
   [renderer.shell.effects :as-alias shell.effects]
   [renderer.shell.handlers :as shell.handlers]
   [renderer.shell.reepl.sci :as shell.reepl.sci]
   [renderer.window.handlers :as window.handlers]))

(rf/reg-event-fx
 ::focus
 (fn [_ _]
   {::shell.effects/focus nil}))

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} _]
   (let [lang (shell.handlers/active-language db)]
     {:db (-> db
              (shell.handlers/init-language)
              (shell.handlers/reset-language-statuses)
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
     (cond-> {:db (-> db
                      (shell.handlers/init-language)
                      (shell.handlers/set-language-status :success))}
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

(rf/reg-event-fx
 ::clear-items
 [persist]
 (fn [{:keys [db]} _]
   {:db (shell.handlers/clear-items db)
    ::shell.effects/welcome (shell.handlers/active-language db)}))

(rf/reg-event-db
 ::toggle-verbose
 [persist]
 (fn [db _]
   (shell.handlers/toggle-verbose db)))

(rf/reg-event-fx
 ::add-item
 [persist]
 (fn [{:keys [db]} [_ item-type value]]
   (cond-> {}
     value
     (assoc :db (shell.handlers/add-item db {:type item-type
                                             :value value}))

     (and value
          (= item-type :error)
          (not (get-in db [:panels :repl-history :visible]))
          (window.handlers/breakpoint? (-> db :window :width) :md))
     (assoc ::app.effects/toast [:error
                                 "Error evaluating expression"
                                 {:description (:cause value)}]))))

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

(rf/reg-event-fx
 ::set-text
 [persist]
 (fn [{:keys [db]} [_ text]]
   {:db (shell.handlers/set-text db text)
    ::shell.effects/focus nil}))

(rf/reg-event-db
 ::clear-completion
 [persist]
 (fn [db _]
   (shell.handlers/clear-completion db)))

(rf/reg-event-db
 ::complete-word
 [persist]
 (fn [db [_ inst]]
   (shell.handlers/complete-word db inst)))

(rf/reg-event-db
 ::set-show-all-completions
 [persist]
 (fn [db [_ show-all?]]
   (shell.handlers/set-show-all-completions db show-all?)))

(rf/reg-event-fx
 ::activate-completion
 [persist]
 (fn [{:keys [db]} [_ index]]
   (let [db (shell.handlers/activate-completion db index)
         completion (-> db :shell :completion)
         {:keys [words pos]} completion
         text (second (get words pos))]
     {:db db
      ::shell.effects/replace-current-word text})))

(rf/reg-event-fx
 ::cycle-completions
 [persist]
 (fn [{:keys [db]} [_ go-back?]]
   (let [db (shell.handlers/cycle-completions db go-back?)
         completion (-> db :shell :completion)
         {:keys [active words pos initial-text]} completion
         text (if active
                (second (get words pos))
                initial-text)]
     {:db db
      ::shell.effects/replace-current-word text})))

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
                                              :current-ns
                                              (shell.reepl.sci/current-ns)
                                              :num (-> db
                                                       shell.handlers/history
                                                       count)}}))
    ::shell.effects/execute {:text text
                             :language (shell.handlers/active-language db)
                             :verbose (shell.handlers/verbose? db)
                             :callback-event [::add-item]}}))
