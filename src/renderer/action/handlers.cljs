(ns renderer.action.handlers
  (:require
   [malli.core :as m]
   [malli.error :as m.error]
   [renderer.action.db
    :as action.db
    :refer [Action ActionGroup ActionGroupId ActionId Shortcut]]
   [renderer.app.db :refer [App]]))

(m/=> entities [:-> App [:vector Action]])
(defn entities
  [db]
  (-> db :actions vals vec))

(m/=> effective-shortcuts [:-> App ActionId [:set Shortcut]])
(defn effective-shortcuts
  [db id]
  (let [key-bindings (:key-bindings db)]
    (if (contains? key-bindings id)
      (get key-bindings id)
      (set (get-in db [:actions id :shortcuts])))))

(m/=> actions-with-shortcuts [:-> App [:vector Action]])
(defn actions-with-shortcuts
  [db]
  (mapv (fn [{:keys [id]
              :as action}]
          (let [shortcuts (effective-shortcuts db id)]
            (if (seq shortcuts)
              (assoc action :shortcuts (vec shortcuts))
              (dissoc action :shortcuts))))
        (entities db)))

(m/=> add-shortcut [:-> App ActionId Shortcut App])
(defn add-shortcut
  [db id shortcut]
  (assoc-in db [:key-bindings id] (conj (effective-shortcuts db id) shortcut)))

(m/=> remove-shortcut [:-> App ActionId Shortcut App])
(defn remove-shortcut
  [db id shortcut]
  (assoc-in db [:key-bindings id] (disj (effective-shortcuts db id) shortcut)))

(m/=> reset-shortcuts [:-> App ActionId App])
(defn reset-shortcuts
  "Removes the user override so the action falls back to its default shortcuts."
  [db id]
  (update db :key-bindings dissoc id))

(m/=> register-action [:-> App Action App])
(defn register-action
  [db action]
  (when-not (action.db/valid-action? action)
    (let [error (-> action action.db/explain-action m.error/humanize)]
      (throw (ex-info (str "Invalid action: " error) {:action action}))))
  (assoc-in db [:actions (:id action)] action))

(m/=> deregister-action [:-> App ActionId App])
(defn deregister-action
  [db id]
  (update db :actions dissoc id))

(m/=> register-action-group [:-> App ActionGroup App])
(defn register-action-group
  [db group]
  (when-not (action.db/valid-action-group? group)
    (let [error (-> group action.db/explain-action-group m.error/humanize)]
      (throw (ex-info (str "Invalid action group: " error) {:group group}))))
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
