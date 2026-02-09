(ns renderer.icon.handlers
  (:require
   [malli.core :as m]
   [malli.error :as m.error]
   [renderer.app.db :refer [App]]
   [renderer.icon.db :as icon.db :refer [Icon IconId]]))

(m/=> register-icon [:-> App Icon App])
(defn register-icon
  [db icon]
  (if-not (icon.db/valid-icon? icon)
    (throw (ex-info (str "Invalid icon: "
                         (-> (icon.db/explain-icon icon)
                             (m.error/humanize)))
                    {:icon icon}))
    (assoc-in db [:icons (:id icon)] icon)))

(m/=> deregister-icon [:-> App IconId App])
(defn deregister-icon
  [db id]
  (if-not (get-in db [:icons id])
    (throw (ex-info "Icon not registered" {:id id}))
    (update db :icons dissoc id)))
