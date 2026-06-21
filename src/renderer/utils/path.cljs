(ns renderer.utils.path
  (:require
   ["paper" :refer [Path]]
   ["svgpath" :as svgpath]
   [clojure.string :as string]
   [malli.core :as m]
   [renderer.db :refer [BooleanOperation
                        PathSegment
                        PathSegments
                        PathCommand
                        PathManipulation
                        PathPointType
                        Vec2]]))

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
      :reverse (.reverse path)
      path)
    (get-d path)))

(m/=> point-indices [:-> [:maybe string?] PathPointType [:maybe Vec2]])
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

(m/=> segment->command [:-> [:maybe PathSegment] [:maybe PathCommand]])
(defn segment->command
  [segment]
  (some-> segment first string/upper-case))

(m/=> segment-point [:-> [:maybe PathSegment] PathPointType [:maybe Vec2]])
(defn segment-point
  [segment point-type]
  (when-let [[xi yi] (some-> segment
                             segment->command
                             (point-indices point-type))]
    [(get segment xi) (get segment yi)]))

(m/=> abs-endpoint [:-> PathSegment [:maybe Vec2] [:maybe Vec2]])
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

(m/=> outgoing-cp [:-> [:maybe PathSegment] [:maybe Vec2]])
(m/=> outgoing-cp [:-> [:maybe PathSegment] [:maybe Vec2]])
(defn outgoing-cp
  "Returns the outgoing control point.
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/d#cubic_b%C3%A9zier_curve"
  [segment]
  (let [command (segment->command segment)]
    (when (contains? #{"C" "S" "Q"} command)
      (let [[ex ey] (segment-point segment :end-point)
            cp (if (= command "C") :end-control-point :start-control-point)
            [cp2x cp2y] (segment-point segment cp)]
        [(- (* 2 ex) cp2x)
         (- (* 2 ey) cp2y)]))))

(m/=> string->segments [:-> [:maybe string?] [:maybe PathSegments]])
(defn string->segments
  [d]
  (some-> d svgpath .abs .-segments))

(m/=> segments->string [:-> PathSegments string?])
(defn segments->string
  [segments]
  (->> segments
       (map #(string/join " " %))
       (string/join " ")))

(m/=> drop-last-segment [:-> PathSegments PathSegments])
(defn drop-last-segment
  [segments]
  (let [n (count segments)]
    (if (> n 1)
      (.slice segments 0 (dec n))
      segments)))

(m/=> acc-endpoints [:-> PathSegments [:vector Vec2]])
(defn acc-endpoints
  [segments]
  (reduce (fn [acc segment]
            (->> (peek acc)
                 (abs-endpoint segment)
                 (conj acc))) [] segments))

(m/=> c->s-segment [:-> PathSegment PathSegment])
(defn c->s-segment
  [segment]
  (let [[_ _cp1x _cp1y cp2x cp2y x y] segment]
    #js ["S" cp2x cp2y x y]))

(m/=> line->q-segment [:-> PathSegment Vec2 PathSegment])
(defn line->q-segment
  "Converts an L or T segment to a quadratic bezier with the control point
   placed at the midpoint of the line."
  [segment prev-ep]
  (let [[_ x y] segment
        [prev-x prev-y] prev-ep
        mid-x (/ (+ prev-x x) 2)
        mid-y (/ (+ prev-y y) 2)]
    #js ["Q" mid-x mid-y x y]))

(m/=> q->c-segment [:-> PathSegment Vec2 PathSegment])
(defn q->c-segment
  [segment prev-ep]
  (let [[_ qcp-x qcp-y x y] segment
        [prev-x prev-y] prev-ep
        cp1x (+ prev-x (* (/ 2.0 3.0) (- qcp-x prev-x)))
        cp1y (+ prev-y (* (/ 2.0 3.0) (- qcp-y prev-y)))
        cp2x (+ x (* (/ 2.0 3.0) (- qcp-x x)))
        cp2y (+ y (* (/ 2.0 3.0) (- qcp-y y)))]
    #js ["C" cp1x cp1y cp2x cp2y x y]))

(defn- segment-for-command
  [segment prev-segment prev-ep command]
  (let [current (segment->command segment)
        [ex ey] (abs-endpoint segment prev-ep)
        [prev-x prev-y] prev-ep]
    (case command
      "L" #js ["L" ex ey]
      "H" #js ["H" ex]
      "V" #js ["V" ey]
      "Q" (case current
            "C" #js ["Q"
                     (/ (+ (aget segment 1) (aget segment 3)) 2)
                     (/ (+ (aget segment 2) (aget segment 4)) 2)
                     ex ey]
            "S" #js ["Q" (aget segment 1) (aget segment 2) ex ey]
            "T" (let [[cpx cpy] (or (outgoing-cp prev-segment) prev-ep)]
                  #js ["Q" cpx cpy ex ey])
            #js ["Q" (/ (+ prev-x ex) 2) (/ (+ prev-y ey) 2) ex ey])
      "C" (case current
            "Q" (q->c-segment segment prev-ep)
            "S" (let [[_ cp2x cp2y] segment
                      [cp1x cp1y] (or (outgoing-cp prev-segment) prev-ep)]
                  #js ["C" cp1x cp1y cp2x cp2y ex ey])
            (-> #js ["L" ex ey]
                (line->q-segment prev-ep)
                (q->c-segment prev-ep)))
      "S" (when (= current "C") (c->s-segment segment))
      nil)))

(m/=> convert-segment [:-> PathSegments int? string? PathSegments])
(defn convert-segment
  "Converts the segment at index to the given command type."
  [segments index command]
  (let [endpoints (acc-endpoints segments)
        prev-ep (get endpoints (dec index))
        segment (aget segments index)]
    (if (= (segment->command segment) command)
      segments
      (let [new-seg (segment-for-command segment
                                         (aget segments (dec index))
                                         prev-ep
                                         command)]
        (if new-seg
          (doto (.slice segments) (aset index new-seg))
          segments)))))

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
