(ns renderer.utils.path
  (:require
   ["paper" :refer [Path]]
   ["svgpath" :as svgpath]
   [clojure.string :as string]
   [malli.core :as m]
   [renderer.db :refer [BooleanOperation PathManipulation Vec2]]))

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
  [command point-type]
  (condp #(and (contains? (first %1) (first %2))
               (= (second %1) (second %2))) [command point-type]
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

(m/=> segment->command [:-> [:maybe [:vector any?]] [:maybe string?]])
(defn segment->command
  [segment]
  (some-> segment first string/upper-case))

(m/=> segment-point [:-> [:maybe [:vector any?]] PointType [:maybe Vec2]])
(defn segment-point
  [segment point-type]
  (when-let [[xi yi] (some-> segment
                             segment->command
                             (point-indices point-type))]
    [(get segment xi) (get segment yi)]))

(m/=> abs-endpoint [:-> [:vector any?] [:maybe Vec2] [:maybe Vec2]])
(defn abs-endpoint
  "Returns the endpoint for an absolute segment.
   For horizontal and vertical line commands the segment only stores one
   coordinate, so we need prev-ep to get the other value."
  [segment prev-ep]
  (case (segment->command segment)
    "H"
    [(second segment) (second prev-ep)]

    "V"
    [(first prev-ep) (second segment)]

    (segment-point segment :end-point)))

(m/=> outgoing-cp [:-> [:maybe [:vector any?]] [:maybe Vec2]])
(defn outgoing-cp
  "Returns the outgoing control point.
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/d#cubic_b%C3%A9zier_curve"
  [segment]
  (let [command (segment->command segment)]
    (when (contains? #{"C" "S"} command)
      (let [[ex ey] (segment-point segment :end-point)
            cp (if (= command "C") :end-control-point :start-control-point)
            [cp2x cp2y] (segment-point segment cp)]
        [(- (* 2 ex) cp2x)
         (- (* 2 ey) cp2y)]))))

(m/=> string->segments [:-> string? [:vector any?]])
(defn string->segments
  [d]
  (-> d svgpath .abs .-segments js->clj))

(def segments->string
  (comp (partial string/join " ")
        flatten))

(m/=> drop-last-segment [:-> [:vector any?] [:vector any?]])
(defn drop-last-segment
  [segments]
  (cond-> segments
    (> (count segments) 1)
    pop))

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
