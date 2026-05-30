(ns renderer.tool.subs
  (:require
   [re-frame.core :as rf]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.impl.base.edit.core :as-alias tool.impl.base.edit]))

(rf/reg-sub
 ::active
 :-> :tool)

(rf/reg-sub
 ::active?
 :<- [::active]
 :=> =)

(rf/reg-sub
 ::editing?
 :<- [::active]
 :-> (partial = ::tool.impl.base.edit/edit))

(rf/reg-sub
 ::not-active?
 :<- [::active]
 :=> not=)

(rf/reg-sub
 ::cached
 :-> :cached-tool)

(rf/reg-sub
 ::cached-or-active
 :<- [::cached]
 :<- [::active]
 (fn [[cached active] _]
   (or cached active)))

(rf/reg-sub
 ::pivot-point
 :-> :pivot-point)

(rf/reg-sub
 ::anchor-offset
 :-> :anchor-offset)

(rf/reg-sub
 ::cursor
 :-> :cursor)

(rf/reg-sub
 ::state
 :-> :state)

(rf/reg-sub
 ::idle?
 :<- [::state]
 :-> (partial = :idle))

(rf/reg-sub
 ::select-box
 :-> :select-box)

(rf/reg-sub
 ::cached-state
 :-> :cached-state)

(rf/reg-sub
 ::help
 :<- [::active]
 :<- [::state]
 :-> (partial apply tool.handlers/help))
