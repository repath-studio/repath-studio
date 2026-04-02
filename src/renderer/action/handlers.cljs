(ns renderer.action.handlers
  (:require
   [malli.core :as m]
   [malli.error :as m.error]
   [renderer.action.db :as action.db :refer [Action ActionId]]
   [renderer.app.db :refer [App]]))

(m/=> entities [:-> App [:* Action]])
(defn entities
  [db]
  (-> db :actions vals))

(m/=> register [:-> App Action App])
(defn register
  [db action]
  (when-not (action.db/valid-action? action)
    (throw (ex-info (str "Invalid action: "
                         (-> (action.db/explain-action action)
                             (m.error/humanize)))
                    {:action action})))
  (assoc-in db [:actions (:id action)] action))

(m/=> deregister [:-> App ActionId App])
(defn deregister
  [db id]
  (if-not (get-in db [:actions id])
    (throw (ex-info "Action not registered" {:id id}))
    (update db :actions dissoc id)))
