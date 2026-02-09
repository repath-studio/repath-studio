(ns renderer.icon.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::icons
 :-> :icons)

(rf/reg-sub
 ::icon
 :<- [::icons]
 (fn [icons [_ id]]
   (get icons id)))
