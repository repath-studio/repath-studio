(ns renderer.icon.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::entities
 :-> :icons)

(rf/reg-sub
 ::path-data
 :<- [::entities]
 (fn [entities [_ id]]
   (get-in entities [id :path])))
