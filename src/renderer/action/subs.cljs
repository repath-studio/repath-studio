(ns renderer.action.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::actions
 :-> :actions)

(rf/reg-sub
 ::action
 :<- [::actions]
 (fn [actions [_ id]]
   (if (= id :separator)
     {:type :separator}
     (get actions id))))

(rf/reg-sub
 ::key-bindings
 :-> :key-bindings)

(rf/reg-sub
 ::action-shortcuts
 :<- [::actions]
 :<- [::key-bindings]
 (fn [[actions key-bindings] [_ id]]
   (let [default-shortcuts (get-in actions [id :shortcuts])]
     (->> (or (get key-bindings id)
              (set default-shortcuts))
          (map #(assoc % :default (boolean (some #{%} default-shortcuts))))))))

(rf/reg-sub
 ::action-groups
 :-> :action-groups)

(rf/reg-sub
 ::groupless-actions
 :<- [::actions]
 :<- [::action-groups]
 (fn [[actions action-groups] _]
   (let [grouped-ids (mapcat :actions (vals action-groups))]
     (apply dissoc actions grouped-ids))))

(rf/reg-sub
 ::action-group
 :<- [::action-groups]
 :=> get)
