(ns renderer.error.events
  (:require
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.events :refer [persist]]
   [renderer.dialog.handlers :as dialog.handlers]
   [renderer.dialog.views :as dialog.views]
   [renderer.error.effects :as-alias error.effects]
   [renderer.i18n.handlers :as i18n.handlers]
   [renderer.menubar.views :as-alias menubar.views]))

(defn reporting-confirmation-dialog-content
  [db]
  [dialog.views/confirmation
   {:content (i18n.handlers/t
              db
              [::reporting-description
               [:div
                [:p "Would you like to help us improve by sending anonymous
                         error reports? You can change your preference at any
                         time from our \"%1\" menu."]
                [:p "For more information, please read our %2."]]]
              [[:strong (i18n.handlers/t db [::menubar.views/help "Help"])]
               [:a.button-link.underline
                {:href "https://repath.studio/policies/privacy/"
                 :target "_blank"}
                (i18n.handlers/t db [::privacy-policy "privacy policy"])]])
    :confirm-action [::set-reporting true]
    :cancel-action [::set-reporting false]
    :cancel-label [::no-thank-you "No, thank you"]}])

(defn reporting-confirmation-dialog
  [db]
  {:title (i18n.handlers/t db [::welcome "Welcome to %1!"] [config/app-name])
   :content [reporting-confirmation-dialog-content db]
   :attrs {:on-open-auto-focus #(.preventDefault %)}})

(rf/reg-event-fx
 ::init-reporting
 (fn [{:keys [db]} _]
   (let [{:keys [error-reporting platform]} db]
     (if (nil? error-reporting)
       {:db (dialog.handlers/create db (reporting-confirmation-dialog db))}
       (let [config (-> config/sentry
                        (assoc :enabled error-reporting)
                        (clj->js))]
         {::error.effects/init-reporting [platform config]})))))

(rf/reg-event-fx
 ::set-reporting
 [persist]
 (fn [{:keys [db]} [_ enabled]]
   {:db (assoc db :error-reporting enabled)
    :fx [(when enabled
           [:dispatch [::init-reporting]])]}))

(rf/reg-event-fx
 ::toggle-reporting
 [persist]
 (fn [{:keys [db]} [_]]
   {:db (update db :error-reporting not)
    :dispatch [::init-reporting]}))
