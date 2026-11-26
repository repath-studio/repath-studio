(ns renderer.snap.db
  (:require
   [renderer.db :refer [Vec2]]))

(def SnapOption
  [:enum :centers :midpoints :corners :nodes :grid :guides])

(def SnapOptions
  [:set SnapOption])

(def NearestNeighbor
  [:map {:closed true}
   [:point Vec2]
   [:base-point Vec2]
   [:dist-squared number?]])

(def AxisMatch
  [:map {:closed true}
   [:axis [:enum :x :y]]
   [:value number?]
   [:point {:optional true} Vec2]
   [:base-point Vec2]
   [:delta number?]
   [:dist number?]
   [:label {:optional true} [:maybe [:tuple keyword? string?]]]])

(def Snap
  [:map {:closed true}
   [:active {:default true} boolean?]
   [:options {:default #{:centers :midpoints :corners :grid :guides}}
    SnapOptions]])
