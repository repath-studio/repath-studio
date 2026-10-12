(ns renderer.db)

(def Vec2
  [:tuple number? number?])

(def BBox
  [:tuple
   [number? {:title "min-x"}]
   [number? {:title "min-y"}]
   [number? {:title "max-x"}]
   [number? {:title "max-y"}]])

(def TimeStamp
  number?)

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

(def TransformOperation
  [:multi {:dispatch (fn [x] (when (seqable? x) (first x)))}
   [:translate [:tuple [:= :translate] number? number?]]
   [:scale [:tuple [:= :scale] number? number?]]
   [:rotate [:tuple [:= :rotate] number? number? number?]]
   [:skew-x [:tuple [:= :skew-x] number?]]
   [:skew-y [:tuple [:= :skew-y] number?]]
   [:matrix [:tuple [:= :matrix]
             number? number? number?
             number? number? number?]]])

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

(def ColorNotation
  [:enum "hex" "rgb" "hsl" "lab" "lch" "oklch" "oklab"])

(def ColorChannel
  [:enum
   "hex"
   "r" "g" "b"
   "h" "s" "l"
   "l" "a" "b"
   "l" "c" "h"
   "alpha"])

(def DomRect
  [:map {:closed true}
   [:x number?]
   [:y number?]
   [:width number?]
   [:height number?]
   [:top number?]
   [:right number?]
   [:bottom number?]
   [:left number?]])

(def Viewbox
  [:tuple
   [number? {:title "x"}]
   [number? {:title "y"}]
   [number? {:title "width"}]
   [number? {:title "height"}]])

(def TransformationMatrix
  [:tuple
   [number? {:title "a"}]
   [number? {:title "b"}]
   [number? {:title "c"}]
   [number? {:title "d"}]
   [number? {:title "e"}]
   [number? {:title "f"}]])
