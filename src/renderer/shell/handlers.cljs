(ns renderer.shell.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [LoadingState]]
   [renderer.shell.db :refer [ShellLanguageId]]))

(m/=> active-language [:-> App ShellLanguageId])
(defn active-language
  [db]
  (get-in db [:shell :active-language]))

(m/=> activate-language [:-> App ShellLanguageId App])
(defn activate-language
  [db language]
  (assoc-in db [:shell :active-language] language))

(m/=> language-status [:-> App ShellLanguageId [:maybe LoadingState]])
(defn language-status
  [db language]
  (get-in db [:shell :languages language :status]))

(m/=> set-language-status [:-> App ShellLanguageId LoadingState App])
(defn set-language-status
  [db language status]
  (assoc-in db [:shell :languages language :status] status))

(m/=> reset-language-statuses [:-> App App])
(defn reset-language-statuses
  [db]
  (->> (keys (-> db :shell :languages))
       (reduce #(update-in %1 [:shell :languages %2] dissoc :status) db)))
