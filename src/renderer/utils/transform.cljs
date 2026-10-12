(ns renderer.utils.transform
  (:require
   [clojure.math :as math]
   [clojure.string :as string]
   [malli.core :as m]
   [renderer.db :refer [BBox TransformationMatrix TransformOperation Vec2]]))

(def EPSILON 1e-6)

(def TransformOperations [:vector TransformOperation])

(def identity-transform [1 0 0 1 0 0])

(m/=> identity? [:-> TransformationMatrix boolean?])
(defn identity?
  [m]
  (every? (fn [[x t]] (< (Math/abs (- x t)) EPSILON))
          (map vector m identity-transform)))

(m/=> point [:-> TransformationMatrix Vec2 Vec2])
(defn point
  [[a b c d e f] [x y]]
  [(+ (* a x) (* c y) e)
   (+ (* b x) (* d y) f)])

(m/=> inverse [:-> TransformationMatrix TransformationMatrix])
(defn inverse
  [[a b c d e f]]
  (let [det (if (zero? (- (* a d) (* b c))) 1 (- (* a d) (* b c)))
        i (/ 1 det)]
    [(* d i)
     (- (* b i))
     (- (* c i))
     (* a i)
     (* i (- (* c f) (* d e)))
     (* i (- (* b e) (* a f)))]))

(m/=> inverse-point [:-> TransformationMatrix Vec2 Vec2])
(defn inverse-point
  [m p]
  (point (inverse m) p))

(m/=> offset [:-> TransformationMatrix Vec2 Vec2])
(defn offset
  [[a b c d _e _f] [x y]]
  (let [[ia ib ic id] (take 4 (inverse [a b c d 0 0]))]
    [(+ (* ia x) (* ic y))
     (+ (* ib x) (* id y))]))

(m/=> aabb [:-> TransformationMatrix BBox BBox])
(defn aabb
  [m [min-x min-y max-x max-y]]
  (let [pts (mapv (partial point m)
                  [[min-x min-y] [min-x max-y] [max-x min-y] [max-x max-y]])]
    [(apply min (map first pts))
     (apply min (map second pts))
     (apply max (map first pts))
     (apply max (map second pts))]))

(m/=> translate [:function
                 [:-> Vec2 TransformationMatrix]
                 [:-> number? number? TransformationMatrix]])
(defn translate
  ([x y]
   [1 0 0 1 x y])
  ([[x y]]
   (translate x y)))

(m/=> scale [:-> Vec2 TransformationMatrix])
(defn scale
  [[sx sy]]
  [sx 0 0 sy 0 0])

(m/=> scale-about [:-> Vec2 Vec2 TransformationMatrix])
(defn scale-about
  [ratio pivot]
  (let [[sx sy] ratio
        [cx cy] pivot]
    [sx 0 0 sy (- cx (* sx cx)) (- cy (* sy cy))]))

(m/=> rotate [:function
              [:-> number? TransformationMatrix]
              [:-> number? number? number? TransformationMatrix]])
(defn rotate
  ([deg]
   (rotate deg 0 0))
  ([deg cx cy]
   (let [t (math/to-radians deg)
         cos (Math/cos t)
         sin (Math/sin t)]
     [cos sin (- sin) cos
      (+ cx (- (* cos cx)) (* sin cy))
      (+ cy (- (* sin cx)) (- (* cos cy)))])))

(m/=> skew-x [:-> number? TransformationMatrix])
(defn skew-x
  [deg]
  [1 0 (Math/tan (math/to-radians deg)) 1 0 0])

(m/=> skew-y [:-> number? TransformationMatrix])
(defn skew-y
  [deg]
  [1 (Math/tan (math/to-radians deg)) 0 1 0 0])

(m/=> compose [:->
               TransformationMatrix TransformationMatrix
               TransformationMatrix])
(defn compose
  [[a1 b1 c1 d1 e1 f1] [a2 b2 c2 d2 e2 f2]]
  [(+ (* a1 a2) (* c1 b2))
   (+ (* b1 a2) (* d1 b2))
   (+ (* a1 c2) (* c1 d2))
   (+ (* b1 c2) (* d1 d2))
   (+ (* a1 e2) (* c1 f2) e1)
   (+ (* b1 e2) (* d1 f2) f1)])

(defn ->number
  [s]
  (when-let [s (string/trim (or s ""))]
    (when (re-matches (re-pattern (str "^[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)"
                                       "([eE][+-]?[0-9]+)?$"))
                      s)
      (js/parseFloat s))))

(defn tokenize
  [s]
  (vec (remove string/blank? (string/split (or s "") (re-pattern "[, ]")))))

(defn ->type
  [s]
  (case (string/lower-case s)
    "translate" :translate
    "scale" :scale
    "rotate" :rotate
    "skewx" :skew-x
    "skewy" :skew-y
    "matrix" :matrix
    nil))

(defn parse-op
  [op-type args]
  (case op-type
    :translate
    (when (<= (count args) 2)
      (let [x (if (seq args) (->number (first args)) 0)
            y (if (>= (count args) 2) (->number (second args)) 0)]
        (when (and (number? x) (number? y))
          [:translate x y])))

    :scale (when (<= (count args) 2)
             (let [sx (if (seq args) (->number (first args)) 1)
                   sy (if (>= (count args) 2) (->number (second args)) sx)]
               (when (and (number? sx) (number? sy))
                 [:scale sx sy])))

    :rotate
    (case (count args)
      1 (let [a (->number (first args))]
          (when (number? a)
            [:rotate a 0 0]))
      3 (let [a (->number (nth args 0))
              cx (->number (nth args 1))
              cy (->number (nth args 2))]
          (when (and (number? a) (number? cx) (number? cy))
            [:rotate a cx cy]))
      nil)

    :skew-x
    (when (= (count args) 1)
      (let [a (->number (first args))]
        (when (number? a)
          [:skew-x a])))

    :skew-y
    (when (= (count args) 1)
      (let [a (->number (first args))]
        (when (number? a)
          [:skew-y a])))

    :matrix
    (when (= (count args) 6)
      (let [m (mapv ->number args)]
        (when (every? number? m)
          (vec (cons :matrix m)))))

    nil))

(defn parse
  [s]
  (when (and (string? s) (not (string/blank? s)))
    (let [matches (re-seq (re-pattern "([a-zA-Z]+)(?:[(](.*?)[)])?") s)
          ops (vec (map (fn [[_ op-type args]]
                          (parse-op (->type op-type) (tokenize args)))
                        matches))]
      (when (and (string/blank? (reduce (fn [acc [full]]
                                          (string/replace acc full ""))
                                        s
                                        matches))
                 (every? some? ops))
        ops))))

(defn op->matrix
  [[op-type & args]]
  (case op-type
    :translate (apply translate args)
    :scale (scale (vec args))
    :rotate (apply rotate args)
    :skew-x (skew-x (first args))
    :skew-y (skew-y (first args))
    (vec args)))

(m/=> ops->matrix [:-> TransformOperations TransformationMatrix])
(defn ops->matrix
  [ops]
  (reduce (fn [acc op]
            (compose acc (op->matrix op)))
          identity-transform
          ops))

(defn snap
  [v]
  (cond
    (< (Math/abs v) EPSILON) 0
    (< (Math/abs (dec v)) EPSILON) 1
    :else v))

(defn close-matrix?
  [m1 m2]
  (every? (fn [[x1 x2]] (< (Math/abs (- x1 x2)) EPSILON))
          (map vector m1 m2)))

(defn linear-decompose
  [[a b c d]]
  (let [det (- (* a d) (* b c))]
    (when (>= (Math/abs det) EPSILON)
      (let [sx (Math/hypot a b)
            theta (Math/atan2 b a)
            sy (/ det sx)
            v1 (+ c (* sy (Math/sin theta)))
            v2 (- d (* sy (Math/cos theta)))
            num (+ (* v1 (Math/cos theta)) (* v2 (Math/sin theta)))]
        {:sx sx
         :theta theta
         :sy sy
         :phi (Math/atan (/ num sx))}))))

(defn rotation-center
  [e f theta]
  (let [cos (Math/cos theta)
        sin (Math/sin theta)
        denom (* 2 (- 1 cos))]
    (when (>= (Math/abs denom) EPSILON)
      [(/ (- (* e (- 1 cos)) (* f sin)) denom)
       (/ (+ (* e sin) (* f (- 1 cos))) denom)])))

(defn op-translate
  [e f]
  (when (or (not (zero? e)) (not (zero? f)))
    [:translate e f]))

(defn op-rotate
  [theta-deg]
  (when-not (zero? theta-deg)
    [:rotate theta-deg 0 0]))

(defn op-scale
  [sx sy]
  (when-not (and (zero? (dec sx)) (zero? (dec sy)))
    [:scale sx sy]))

(defn op-skew
  [phi-deg]
  (when-not (zero? phi-deg)
    [:skew-x phi-deg]))

(defn centered-rotate
  [m e f theta theta-deg]
  (let [c (rotation-center e f theta)
        c-ops (when c [[:rotate theta-deg (snap (first c)) (snap (second c))]])]
    (when (and c-ops (close-matrix? m (ops->matrix c-ops)))
      c-ops)))

(defn- build-ops
  [[a b c d e f] lin]
  (let [e (snap e)
        f (snap f)
        theta (:theta lin)
        theta-deg (snap (math/to-degrees theta))
        translate-op (op-translate e f)
        rotate-op (op-rotate theta-deg)
        scale-op (op-scale (snap (:sx lin)) (snap (:sy lin)))
        skew-op (op-skew (snap (math/to-degrees (:phi lin))))
        rotation-only? (and (not (zero? theta-deg))
                            (nil? scale-op)
                            (nil? skew-op))]
    (if rotation-only?
      (or (centered-rotate [a b c d e f] e f theta theta-deg)
          (vec (filter some? [translate-op rotate-op])))
      (vec (filter some? [translate-op rotate-op scale-op skew-op])))))

(m/=> matrix->ops [:-> TransformationMatrix TransformOperations])
(defn matrix->ops
  [[a b c d e f]]
  (if-let [lin (linear-decompose [a b c d])]
    (let [e (snap e)
          f (snap f)
          ops (build-ops [a b c d e f] lin)]
      (if (close-matrix? [a b c d e f] (ops->matrix ops))
        ops
        [[:matrix a b c d e f]]))
    [[:matrix a b c d e f]]))

(defn ->num
  [v]
  (js/parseFloat (.toFixed v 3)))

(defn op->string
  [[op-type & args]]
  (case op-type
    :translate
    (str "translate(" (->num (first args))
         (when (not (zero? (second args)))
           (str " " (->num (second args))))
         ")")

    :scale
    (if (zero? (- (first args) (second args)))
      (str "scale(" (->num (first args)) ")")
      (str "scale(" (->num (first args)) " " (->num (second args)) ")"))

    :rotate
    (str "rotate(" (->num (first args))
         (when (or (not (zero? (nth args 1)))
                   (not (zero? (nth args 2))))
           (str " " (->num (nth args 1)) " " (->num (nth args 2))))
         ")")

    :skew-x
    (str "skewX(" (->num (first args)) ")")

    :skew-y
    (str "skewY(" (->num (first args)) ")")

    (str (name op-type) "(" (string/join " " (map ->num args)) ")")))

(defn identity-op?
  [[op-type & args]]
  (let [args (mapv ->num args)]
    (case op-type
      (:rotate :skew-x :skew-y)
      (zero? (first args))

      :translate
      (every? zero? args)

      :scale
      (every? #(zero? (dec %)) args)

      :matrix
      (identity? (vec args))

      false)))

(m/=> ops->string [:-> TransformOperations [:maybe string?]])
(defn ops->string
  [ops]
  (let [ops (vec (remove identity-op? ops))]
    (when (seq ops)
      (string/join " " (map op->string ops)))))

(m/=> compose-attr [:->
                    [:maybe string?] [:maybe TransformationMatrix]
                    [:maybe string?]])
(defn compose-attr
  [value op]
  (let [op (or op identity-transform)
        m (compose op (ops->matrix (or (parse value) [])))]
    (when-not (identity? m)
      (ops->string (matrix->ops m)))))

(m/=> ->str [:-> TransformationMatrix [:maybe string?]])
(defn ->str
  [m]
  (ops->string (matrix->ops m)))

(m/=> unscale [:-> TransformationMatrix TransformationMatrix])
(defn unscale
  [m]
  (ops->matrix (vec (remove #(= :scale (first %)) (matrix->ops m)))))

(m/=> unscale-at [:-> TransformationMatrix Vec2 TransformationMatrix])
(defn unscale-at
  [m p]
  (let [u (unscale m)
        [a b c d _e _f] u
        [px py] p
        [qx qy] (point m p)]
    [a b c d
     (- qx (+ (* a px) (* c py)))
     (- qy (+ (* b px) (* d py)))]))

(m/=> scale-factor [:-> TransformationMatrix number?])
(defn scale-factor
  [[a b c d _e _f]]
  (Math/sqrt (* (Math/hypot a b) (Math/hypot c d))))
