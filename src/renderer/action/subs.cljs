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
 ::action-groups
 :-> :action-groups)

(rf/reg-sub
 ::action-group
 :<- [::action-groups]
 (fn [groups [_ id]]
   (get groups id)))
