(ns renderer.a11y.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::a11y
 :-> :a11y)

(rf/reg-sub
 ::filters
 :<- [::a11y]
 :-> :filters)

(rf/reg-sub
 ::active-filter
 :<- [::a11y]
 :-> :active-filter)

(rf/reg-sub
 ::filter-active?
 :<- [::active-filter]
 (fn [active-filter [_ k]]
   (= active-filter k)))
