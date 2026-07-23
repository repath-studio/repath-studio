(ns renderer.action.handlers
  (:require
   [malli.core :as m]
   [renderer.action.db :refer [Action ActionGroup ActionGroupId ActionId]]
   [renderer.app.db :refer [App]]))

(m/=> entities [:-> App [:vector Action]])
(defn entities
  [db]
  (-> db :actions vals vec))

(m/=> register-action [:-> App Action App])
(defn register-action
  [db action]
  (assoc-in db [:actions (:id action)] action))

(m/=> deregister-action [:-> App ActionId App])
(defn deregister-action
  [db id]
  (update db :actions dissoc id))

(m/=> register-action-group [:-> App ActionGroup App])
(defn register-action-group
  [db group]
  (assoc-in db [:action-groups (:id group)] group))

(m/=> deregister-action-group [:-> App ActionGroupId App])
(defn deregister-action-group
  [db id]
  (update db :action-groups dissoc id))

(m/=> add-action-to-group [:-> App ActionGroupId ActionId App])
(defn add-action-to-group
  [db group-id action-id]
  (let [actions (get-in db [:action-groups group-id :actions])]
    (cond-> db
      (not (some #{action-id} actions))
      (update-in [:action-groups group-id :actions] conj action-id))))

(m/=> remove-action-from-group [:-> App ActionGroupId ActionId App])
(defn remove-action-from-group
  [db group-id action-id]
  (->> (partial filterv (complement #{action-id}))
       (update-in db [:action-groups group-id :actions])))
