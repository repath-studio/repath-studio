(ns renderer.utils.math
  (:require
   [clojure.math :as math]
   [malli.core :as m]
   [renderer.db :refer [Vec2]]))

(def KAPPA
  "Constant for converting a quarter-circle/ellipse arc to a cubic Bézier curve.
   Calculated as: 4/3 * (√2 - 1)"
  0.5522847498)

(m/=> clamp [:-> number? number? number? number?])
(defn clamp
  "Clamps a number within the provided bounds."
  [x minimum maximum]
  (-> x
      (max minimum)
      (min maximum)))

(m/=> angle-dx [:-> number? number? number?])
(defn angle-dx
  [degrees radius]
  (-> degrees
      (math/to-radians)
      (Math/cos)
      (* radius)))

(m/=> angle-dy [:-> number? number? number?])
(defn angle-dy
  [degrees radius]
  (-> degrees
      (math/to-radians)
      (Math/sin)
      (* radius)))

(m/=> normalize-angle [:-> number? number?])
(defn normalize-angle
  "Normalizes an angle to be in range [0-2PI). Angles outside this range will
   be normalized to be the equivalent angle with that range."
  [angle]
  (mod angle (* 2 Math/PI)))

(m/=> angle [:-> Vec2 Vec2 number?])
(defn angle
  "Calculates the angle between two points."
  [[x1 y1] [x2 y2]]
  (let [delta-y (- y2 y1)
        delta-x (- x2 x1)]
    (-> (Math/atan2 delta-y delta-x)
        (normalize-angle)
        (math/to-degrees))))

(m/=> distance [:-> Vec2 Vec2 number?])
(defn distance
  [[x1 y1] [x2 y2]]
  (math/hypot (- x2 x1) (- y2 y1)))

(m/=> mean [:-> [:+ number?] number?])
(defn mean
  [& n]
  (/ (apply + n)
     (count n)))

(m/=> elementwise [:-> fn? vector? [:or [:vector number?] number?] vector?])
(defn elementwise
  ([f v x]
   (if (number? x)
     (mapv f v (repeat x))
     (mapv f v x)))
  ([op v x & more]
   (apply mapv op (map #(cond-> % (number? %) repeat)
                       (cons v (cons x more))))))

(m/=> v-add [:-> vector? [:+ [:or [:vector number?] number?]] vector?])
(defn v-add
  ([v x] (elementwise + v x))
  ([v x & more] (apply elementwise + v x more)))

(m/=> v-sub [:-> vector? [:+ [:or [:vector number?] number?]] vector?])
(defn v-sub
  ([v x] (elementwise - v x))
  ([v x & more] (apply elementwise - v x more)))

(m/=> v-mul [:-> vector? [:+ [:or [:vector number?] number?]] vector?])
(defn v-mul
  ([v x] (elementwise * v x))
  ([v x & more] (apply elementwise * v x more)))

(m/=> v-div [:-> vector? [:+ [:or [:vector number?] number?]] vector?])
(defn v-div
  ([v x] (elementwise / v x))
  ([v x & more] (apply elementwise / v x more)))

(m/=> v-dot [:-> [:vector number?] [:vector number?] number?])
(defn v-dot
  [v x]
  (reduce + (map * v x)))
