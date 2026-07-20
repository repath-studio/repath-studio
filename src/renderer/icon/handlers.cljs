(ns renderer.icon.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.icon.db :refer [Icon IconId]]))

(m/=> register-icon [:-> App Icon App])
(defn register-icon
  [db icon]
  (assoc-in db [:icons (:id icon)] icon))

(m/=> deregister-icon [:-> App IconId App])
(defn deregister-icon
  [db id]
  (update db :icons dissoc id))
