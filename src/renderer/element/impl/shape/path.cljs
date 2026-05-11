(ns renderer.element.impl.shape.path
  "https://www.w3.org/TR/SVG/paths.html#PathElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/path"
  (:require
   ["svg-path-bbox" :refer [svgPathBbox]]
   ["svgpath" :as svgpath]
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [renderer.db :refer [PathSegment PathSegments PathPointType Vec2]]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.hierarchy :as hierarchy]
   [renderer.input.handlers :as input.handlers]
   [renderer.tool.views :as tool.views]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]
   [renderer.utils.path :as utils.path]
   [renderer.utils.svg :as utils.svg]))

(hierarchy/derive! :path ::element.hierarchy/shape)

(defmethod element.hierarchy/properties :path
  []
  {:icon "bezier-curve"
   :label [::label "Path"]
   :description [::description
                 "The <path> SVG element is the generic element to define a
                  shape. All the basic shapes can be created with a path
                  element."]
   :attrs [:stroke-width
           :fill
           :stroke
           :stroke-linejoin
           :stroke-linecap
           :opacity]})

(defmethod element.hierarchy/translate :path
  [el [x y]]
  (update-in el [:attrs :d] #(some-> %
                                     (svgpath)
                                     (.translate x y)
                                     (.toString))))

(defmethod element.hierarchy/scale :path
  [el ratio pivot-point]
  (let [[scale-x scale-y] ratio
        offset (utils.element/scale-offset ratio pivot-point)
        [x y] (element.hierarchy/bbox el)
        [x y] (-> (matrix/add [x y] offset)
                  (matrix/sub (matrix/mul ratio [x y])))]
    (update-in el [:attrs :d] #(some-> %
                                       (svgpath)
                                       (.scale scale-x scale-y)
                                       (.translate x y)
                                       (.toString)))))

(defmethod element.hierarchy/bbox :path
  [el]
  (some-> el :attrs :d svgPathBbox vec))

(m/=> ->px-point [:-> PathSegment PathPointType [:maybe Vec2]])
(defn ->px-point
  [segment point-type]
  (some->> (utils.path/segment-point segment point-type)
           (mapv utils.length/unit->px)))

(defn render-arms
  [endpoints offset index segment]
  (let [prev-ep (some-> (get endpoints (dec index)) (matrix/add offset))
        cp0 (some-> segment
                    (->px-point :start-control-point)
                    (matrix/add offset))
        ep (some-> segment
                   (->px-point :end-point)
                   (matrix/add offset))]
    (case (utils.path/segment->command segment)
      "C"
      (let [cp1 (matrix/add (->px-point segment :end-control-point) offset)]
        [:<>
         (when prev-ep [utils.svg/arm prev-ep cp0])
         [utils.svg/arm cp1 ep]])

      "S"
      [utils.svg/arm cp0 ep]

      `"Q"
      [:<>
       (when prev-ep [utils.svg/arm prev-ep cp0])
       [utils.svg/arm cp0 ep]]

      nil)))

(m/=> handles [:-> [:vector Vec2] int? PathSegment [:maybe [:vector map?]]])
(defn handles
  [endpoints index segment]
  (let [command (utils.path/segment->command segment)
        prev-ep (get endpoints (dec index))]
    (case command
      ("M" "L" "T" "A")
      [{:point-type :end-point
        :pos (->px-point segment :end-point)}]

      "C"
      [(when prev-ep
         {:point-type :start-control-point
          :pos (->px-point segment :start-control-point)
          :rounded true})
       {:point-type :end-control-point
        :pos (->px-point segment :end-control-point)
        :rounded true}
       {:point-type :end-point
        :pos (->px-point segment :end-point)}]

      ("S" "Q")
      [{:point-type :start-control-point
        :pos (->px-point segment :start-control-point)
        :rounded true}
       {:point-type :end-point
        :pos (->px-point segment :end-point)}]

      "H"
      [(when-let [[_ prev-y] prev-ep]
         {:point-type :end-point
          :pos (->> [(second segment) prev-y]
                    (mapv utils.length/unit->px))})]

      "V"
      [(when-let [[prev-x _] prev-ep]
         {:point-type :end-point
          :pos (->> [prev-x (second segment)]
                    (mapv utils.length/unit->px))})]

      nil)))

(defn render-handles
  [{:keys [element-id endpoints offset]} index segment]
  (->> (handles endpoints index segment)
       (map (fn [{:keys [point-type pos rounded]}]
              (let [[ax ay] (matrix/add offset pos)]
                [tool.views/handle {:id (keyword index point-type)
                                    :x ax
                                    :y ay
                                    :type :handle
                                    :action :edit
                                    :rounded (boolean rounded)
                                    :element-id element-id}])))
       (into [:g])))

(m/=> acc-endpoints [:-> PathSegments [:vector Vec2]])
(defn acc-endpoints
  [segments]
  (reduce (fn [acc segment]
            (->> (peek acc)
                 (utils.path/abs-endpoint segment)
                 (conj acc))) [] segments))

(defmethod element.hierarchy/render-edit :path
  [el]
  (let [offset (utils.element/offset el)
        segments (->> el :attrs :d utils.path/string->segments)
        endpoints (acc-endpoints segments)]
    [:g
     (->> segments
          (map-indexed (partial render-arms endpoints offset))
          (into [:g]))
     (->> segments
          (map-indexed (partial render-handles {:element-id (:id el)
                                                :endpoints endpoints
                                                :offset offset}))
          (into [:g]))]))

(m/=> translate-seg-point [:-> PathSegments number? PathPointType Vec2
                           PathSegment])
(defn translate-seg-point
  [segments index point-type delta]
  (let [segment (get segments index)
        [dx dy] delta
        cmd (utils.path/segment->command segment)]
    (case cmd
      "H"
      (cond-> segments
        (= point-type :end-point)
        (assoc index (assoc segment 1 (-> (second segment)
                                          (utils.length/transform + dx)))))

      "V"
      (cond-> segments
        (= point-type :end-point)
        (assoc index (assoc segment 1 (-> (second segment)
                                          (utils.length/transform + dy)))))

      (let [[xi yi] (utils.path/point-indices cmd point-type)]
        (cond-> segments
          (and xi yi)
          (assoc index
                 (assoc segment
                        xi (-> (get segment xi)
                               (utils.length/transform + dx))
                        yi (-> (get segment yi)
                               (utils.length/transform + dy)))))))))

(m/=> translate-point [:-> int? PathPointType Vec2 PathSegments PathSegments])
(defn translate-point
  [index point-type delta segments]
  (let [segments (translate-seg-point segments index point-type delta)]
    (cond-> segments
      (= point-type :end-point)
      (-> (translate-seg-point index :end-control-point delta)
          (translate-seg-point (inc index) :start-control-point delta)))))

(defmethod element.hierarchy/edit :path
  [el offset handle lock?]
  (let [offset (cond-> offset lock? input.handlers/lock-direction)
        index (js/parseInt (namespace handle))
        point-type (keyword (name handle))]
    (update-in el [:attrs :d]
               #(->> (utils.path/string->segments %)
                     (translate-point index point-type offset)
                     (utils.path/segments->string)))))

(defmethod element.hierarchy/path :path
  [el]
  (-> el :attrs :d))
