(ns renderer.snap.db
  (:require
   [renderer.db :refer [Vec2]]))

(def SnapOption
  [:enum :centers :midpoints :corners :nodes :alignment :grid :guides])

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
   [:options {:default #{:centers :midpoints :corners :alignment :grid :guides}}
    SnapOptions]])

(def Snapping
  [:map {:closed true}
   [:kdtree {:optional true} [:maybe map?]]
   [:viewbox-kdtree {:optional true} [:maybe map?]]
   [:x-tree {:optional true} [:maybe map?]]
   [:y-tree {:optional true} [:maybe map?]]
   [:snapping-points-cache {:optional true} map?]
   [:nearest-neighbor-offset {:optional true} [:maybe Vec2]]
   [:nearest-neighbor {:optional true} [:maybe NearestNeighbor]]
   [:nearest-neighbors {:optional true} [:sequential NearestNeighbor]]
   [:nearest-x {:optional true} [:maybe AxisMatch]]
   [:nearest-y {:optional true} [:maybe AxisMatch]]])
