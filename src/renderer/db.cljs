(ns renderer.db)

(def Vec2
  [:tuple number? number?])

(def BBox
  [:tuple
   [number? {:title "min-x"}]
   [number? {:title "min-y"}]
   [number? {:title "max-x"}]
   [number? {:title "max-y"}]])

(def Orientation
  [:enum :vertical :horizontal])

(def LoadingState
  [:enum :loading :success :error])

(def JS_Element
  [:fn (fn [x] (instance? js/Element x))])

(def JS_Object
  [:fn (fn [x] (instance? js/Object x))])

(def JS_Array
  [:fn (fn [x] (instance? js/Array x))])

(def JS_Promise
  [:fn (fn [x] (instance? js/Promise x))])

(def BooleanOperation
  [:enum :unite :intersect :subtract :exclude :divide])

(def PathManipulation
  [:enum :simplify :smooth :flatten :reverse])

(def PathPointType
  [:enum
   :start-control-point
   :end-control-point
   :end-point])

(def PathCommand
  [:enum
   "M" "m"
   "L" "l"
   "H" "h"
   "V" "v"
   "C" "c"
   "S" "s"
   "Q" "q"
   "T" "t"
   "A" "a"
   "Z" "z"])

(def PathSegment JS_Array)

(def PathSegments JS_Array)
