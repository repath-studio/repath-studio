(ns renderer.snap.db
  (:require [renderer.db :refer [Vec2]]))

(def SnapOption
  [:enum :centers :midpoints :corners :nodes :grid :guides])

(def SnapOptions
  [:set SnapOption])

(def NearestNeighbor
  [:map {:closed true}
   [:point Vec2]
   [:base-point Vec2]
   [:dist-squared number?]])

(def Snap
  [:map {:closed true}
   [:active {:default false} boolean?]
   [:transient-active {:default false} boolean?]
   [:threshold {:default 15} number?]
   [:options {:default #{:centers :midpoints :corners :nodes}} SnapOptions]])
