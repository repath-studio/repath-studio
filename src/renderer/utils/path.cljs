(ns renderer.utils.path
  (:require
   ["paper" :refer [Path]]
   ["svgpath" :as svgpath]
   [clojure.string :as string]
   [malli.core :as m]
   [renderer.db :refer [BooleanOperation JS_Array PathManipulation Vec2]]))

(m/=> get-d [:-> any? string?])
(defn get-d
  [paper-path]
  (-> paper-path
      (.exportSVG)
      (.getAttribute "d")))

(m/=> manipulate [:-> string? PathManipulation string?])
(defn manipulate
  [path manipulation]
  (let [path (Path. path)]
    (case manipulation
      :simplify (.simplify path)
      :smooth (.smooth path)
      :flatten (.flatten path)
      :reverse (.reverse path))
    (get-d path)))

(def PointType
  [:enum
   :start-control-point
   :end-control-point
   :end-point])

(m/=> point-indices [:-> string? PointType [:maybe Vec2]])
(defn point-indices
  "Returns the vector indices if the point-type is defined for the command.
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/d#path_commands"
  [cmd point-type]
  (condp #(and (contains? (first %1) (first %2))
               (= (second %1) (second %2))) [cmd point-type]
    [#{"M" "L" "T"} :end-point]
    [1 2]

    [#{"S" "Q"} :end-point]
    [3 4]

    [#{"C"} :end-point]
    [5 6]

    [#{"A"} :end-point]
    [6 7]

    [#{"C" "S" "Q"} :start-control-point]
    [1 2]

    [#{"C"} :end-control-point]
    [3 4]

    nil))

(m/=> segment->cmd [:-> [:maybe JS_Array] [:maybe string?]])
(defn segment->cmd
  [seg]
  (some-> seg (aget 0) string/upper-case))

(m/=> segment-point [:-> [:maybe JS_Array] PointType [:maybe Vec2]])
(defn segment-point
  [seg point-type]
  (when-let [[xi yi] (some-> seg segment->cmd (point-indices point-type))]
    [(aget seg xi) (aget seg yi)]))

(m/=> abs-endpoint [:-> JS_Array [:maybe Vec2] [:maybe Vec2]])
(defn abs-endpoint
  "Returns the endpoint for an absolute segment.
   For horizontal and vertical line commands the segment only stores one
   coordinate, so we need prev-ep to get the other value."
  [seg prev-ep]
  (case (segment->cmd seg)
    "H" [(aget seg 1) (second prev-ep)]
    "V" [(first prev-ep) (aget seg 1)]
    (segment-point seg :end-point)))

(m/=> outgoing-cp [:-> [:maybe JS_Array] [:maybe Vec2]])
(defn outgoing-cp
  "Returns the outgoing control point.
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/d#cubic_b%C3%A9zier_curve"
  [seg]
  (let [cmd (segment->cmd seg)]
    (when (contains? #{"C" "S"} cmd)
      (let [[ex ey] (segment-point seg :end-point)
            cp (if (= cmd "C") :end-control-point :start-control-point)
            [cp2x cp2y] (segment-point seg cp)]
        [(- (* 2 ex) cp2x)
         (- (* 2 ey) cp2y)]))))

(m/=> last-seg [:-> string? [:maybe [:vector string?]]])
(defn last-seg
  [d]
  (let [segs (-> d svgpath .abs .-segments)]
    (aget segs (dec (.-length segs)))))

(m/=> boolean-operation [:-> string? string? BooleanOperation string?])
(defn boolean-operation
  [path-a path-b operation]
  (let [path-a (Path. path-a)
        path-b (Path. path-b)]
    (get-d (case operation
             :unite (.unite path-a path-b)
             :intersect (.intersect path-a path-b)
             :subtract (.subtract path-a path-b)
             :exclude (.exclude path-a path-b)
             :divide (.divide path-a path-b)))))
