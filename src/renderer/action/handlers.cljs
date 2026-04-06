(ns renderer.action.handlers
  (:require
   [malli.core :as m]
   [malli.error :as m.error]
   [renderer.action.db
    :as action.db
    :refer [Action ActionGroup ActionGroupId ActionId]]
   [renderer.app.db :refer [App]]))

(m/=> entities [:-> App [:* Action]])
(defn entities
  [db]
  (-> db :actions vals))

(m/=> register-action [:-> App Action App])
(defn register-action
  [db action]
  (when-not (action.db/valid-action? action)
    (throw (ex-info (str "Invalid action: "
                         (-> (action.db/explain-action action)
                             (m.error/humanize)))
                    {:action action})))
  (assoc-in db [:actions (:id action)] action))

(m/=> deregister-action [:-> App ActionId App])
(defn deregister-action
  [db id]
  (if-not (get-in db [:actions id])
    (throw (ex-info "Action not registered" {:id id}))
    (update db :actions dissoc id)))

(m/=> register-action-group [:-> App ActionGroup App])
(defn register-action-group
  [db group]
  (when-not (action.db/valid-action-group? group)
    (throw (ex-info (str "Invalid action group: "
                         (-> (action.db/explain-action-group group)
                             (m.error/humanize)))
                    {:group group})))
  (assoc-in db [:action-groups (:id group)] group))

(m/=> deregister-action-group [:-> App ActionGroupId App])
(defn deregister-action-group
  [db id]
  (if-not (get-in db [:action-groups id])
    (throw (ex-info "Action group not registered" {:id id}))
    (update db :action-groups dissoc id)))

(m/=> add-action-to-group [:-> App ActionGroupId ActionId App])
(defn add-action-to-group
  [db group-id action-id]
  (update-in db [:action-groups group-id :actions] conj action-id))

(m/=> remove-action-from-group [:-> App ActionGroupId ActionId App])
(defn remove-action-from-group
  [db group-id action-id]
  (->> (complement #{action-id})
       (update-in db [:action-groups group-id :actions] filterv)))
