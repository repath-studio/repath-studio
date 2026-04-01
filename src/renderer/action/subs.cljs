(ns renderer.action.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::registry
 :-> :actions)

(rf/reg-sub
 ::action
 :<- [::registry]
 (fn [registry [_ id]]
   (if (= id :separator)
     {:type :separator}
     (get registry id))))
