(ns renderer.input.subs
  (:require
   [re-frame.core :as rf]
   [renderer.tool.handlers :as tool.handlers]))

(rf/reg-sub
 ::active-pointers
 :-> :active-pointers)

(rf/reg-sub
 ::pinch-distance
 :-> :pinch-distance)

(rf/reg-sub
 ::pointer-pos
 :-> :pointer-pos)

(rf/reg-sub
 ::adjusted-pointer-pos
 :-> :adjusted-pointer-pos)

(rf/reg-sub
 ::pointer-offset
 :-> :pointer-offset)

(rf/reg-sub
 ::drag-pointer
 :-> :drag-pointer)

(rf/reg-sub
 ::adjusted-pointer-offset
 :-> :adjusted-pointer-offset)

(rf/reg-sub
 ::drag?
 :<- [::drag-pointer]
 :-> boolean)

(rf/reg-sub
 ::snapped-position
 :-> tool.handlers/snapped-position)

(rf/reg-sub
 ::snapped-offset
 :-> tool.handlers/snapped-offset)
