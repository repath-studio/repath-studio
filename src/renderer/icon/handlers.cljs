(ns renderer.icon.handlers
  (:require
   [malli.core :as m]
   [malli.error :as m.error]
   [renderer.app.db :refer [App]]
   [renderer.icon.db :as icon.db :refer [Icon IconId]]))

(m/=> register-icon [:-> App Icon App])
(defn register-icon
  [db icon]
  (when-not (icon.db/valid-icon? icon)
    (let [error (-> icon icon.db/explain-icon m.error/humanize)]
      (throw (ex-info (str "Invalid icon: " error) {:icon icon}))))
  (assoc-in db [:icons (:id icon)] icon))

(m/=> deregister-icon [:-> App IconId App])
(defn deregister-icon
  [db id]
  (update db :icons dissoc id))
