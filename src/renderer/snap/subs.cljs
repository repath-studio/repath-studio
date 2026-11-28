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
   (:active snap)))

(rf/reg-sub
 ::options
 :<- [::snap]
 :-> :options)

(rf/reg-sub
 ::option-enabled?
 :<- [::options]
 :=> contains?)

(rf/reg-sub
 ::snapping
 :-> :snapping)

(rf/reg-sub
 ::nearest-neighbor
 :<- [::snapping]
 :-> :nearest-neighbor)

(rf/reg-sub
 ::nearest-x
 :<- [::snapping]
 :-> :nearest-x)

(rf/reg-sub
 ::nearest-y
 :<- [::snapping]
 :-> :nearest-y)
