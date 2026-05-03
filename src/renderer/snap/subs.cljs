(ns renderer.snap.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::snap
 :-> :snap)

(rf/reg-sub
 ::active?
 :<- [::snap]
 (fn [snap _]
   (or (:active snap)
       (:transient-active snap))))

(rf/reg-sub
 ::options
 :<- [::snap]
 :-> :options)

(rf/reg-sub
 ::option-enabled?
 :<- [::options]
 :=> contains?)

(rf/reg-sub
 ::nearest-neighbor
 :-> :nearest-neighbor)
