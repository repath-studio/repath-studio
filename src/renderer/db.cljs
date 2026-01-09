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

(def BooleanOperation
  [:enum :unite :intersect :subtract :exclude :divide])

(def PathManipulation
  [:enum :simplify :smooth :flatten :reverse])

(def JS_Element
  [:fn (fn [x] (instance? js/Element x))])

(def JS_Object
  [:fn (fn [x] (instance? js/Object x))])

(def JS_Array
  [:fn (fn [x] (instance? js/Array x))])

(def JS_Promise
  [:fn (fn [x] (instance? js/Promise x))])

(def a-series-paper-sizes
  {0 [2384 3370]
   1 [1684 2384]
   2 [1191 1684]
   3 [842 1191]
   4 [595 842]
   5 [420 595]
   6 [298 420]
   7 [210 298]
   8 [147 210]
   9 [105 147]
   10 [74 105]})
