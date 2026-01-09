(ns renderer.tool.subs
  (:require
   [re-frame.core :as rf]
   [renderer.tool.handlers :as tool.handlers]))

(rf/reg-sub
 ::active
 :-> :tool)

(rf/reg-sub
 ::cached
 :-> :cached-tool)

(rf/reg-sub
 ::pivot-point
 :-> :pivot-point)

(rf/reg-sub
 ::drag-pointer
 :-> :drag-pointer)

(rf/reg-sub
 ::drag?
 :<- [::drag-pointer]
 boolean)

(rf/reg-sub
 ::cursor
 :-> :cursor)

(rf/reg-sub
 ::state
 :-> :state)

(rf/reg-sub
 ::cached-state
 :-> :cached-state)

(rf/reg-sub
 ::help
 :<- [::active]
 :<- [::state]
 (fn [[tool state] _]
   (tool.handlers/help tool state)))
