(ns renderer.a11y.subs
  (:require
   [re-frame.core :as rf]
   [renderer.a11y.events :as-alias a11y.events]))

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

(rf/reg-sub
 ::filter-actions
 :<- [::filters]
 (fn [filters _]
   (->> filters
        (mapv (fn [{:keys [id label]}]
                {:id id
                 :label label
                 :icon "a11y"
                 :active [::filter-active? id]
                 :event [::a11y.events/toggle-active-filter id]})))))
