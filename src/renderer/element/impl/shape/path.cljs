(ns renderer.element.impl.shape.path
  "https://www.w3.org/TR/SVG/paths.html#PathElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/path"
  (:require
   ["svg-path-bbox" :refer [svgPathBbox]]
   ["svgpath" :as svgpath]
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [renderer.attribute.impl.d :as attribute.impl.d]
   [renderer.db :refer [PathSegment PathSegments PathPointType Vec2]]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.hierarchy :as hierarchy]
   [renderer.input.handlers :as input.handlers]
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
                                     (.round 3)
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
                                       (.round 3)
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
  [{:keys [endpoints segments offset]} index segment]
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
      [:<>
       (when-let [implied-cp1 (some-> (aget segments (dec index))
                                      utils.path/outgoing-cp
                                      (matrix/add offset))]
         [utils.svg/arm prev-ep implied-cp1])
       [utils.svg/arm cp0 ep]]

      `"Q"
      [:<>
       (when prev-ep [utils.svg/arm prev-ep cp0])
       [utils.svg/arm cp0 ep]]

      nil)))

(m/=> handles [:->
               [:vector Vec2] PathSegments int? PathSegment
               [:maybe [:vector map?]]])
(defn handles
  [endpoints segments index segment]
  (let [command (utils.path/segment->command segment)
        prev-ep (get endpoints (dec index))]
    (case command
      ("M" "L" "T" "A")
      [{:point-type :end-point
        :pos (->px-point segment :end-point)}]

      "C"
      [{:point-type :end-point
        :pos (->px-point segment :end-point)}
       (when prev-ep
         {:point-type :start-control-point
          :pos (->px-point segment :start-control-point)
          :rounded true})
       {:point-type :end-control-point
        :pos (->px-point segment :end-control-point)
        :rounded true}]

      "S"
      (let [cp0 (->px-point segment :start-control-point)
            implied-cp1 (some-> (aget segments (dec index))
                                utils.path/outgoing-cp
                                (->> (mapv utils.length/unit->px)))]
        (cond-> [{:point-type :end-point
                  :pos (->px-point segment :end-point)}
                 {:point-type :start-control-point
                  :pos cp0
                  :rounded true}]
          implied-cp1 (conj {:point-type :implied-control-point
                             :pos implied-cp1
                             :rounded true
                             :implied true})))

      "Q"
      [{:point-type :end-point
        :pos (->px-point segment :end-point)}
       {:point-type :start-control-point
        :pos (->px-point segment :start-control-point)
        :rounded true}]

      "H"
      [(when-let [[_ prev-y] prev-ep]
         {:point-type :end-point
          :cursor "ew-resize"
          :pos (mapv utils.length/unit->px [(second segment) prev-y])})]

      "V"
      [(when-let [[prev-x _] prev-ep]
         {:point-type :end-point
          :cursor "ns-resize"
          :pos (mapv utils.length/unit->px [prev-x (second segment)])})]

      nil)))

(defn segment-handles
  [{:keys [parent endpoints segments offset]} index segment]
  (->> (handles endpoints segments index segment)
       (mapv (fn [{:keys [point-type pos rounded implied cursor]}]
               (let [label (-> (utils.path/segment->command segment)
                               (attribute.impl.d/path-commands)
                               :label)]
                 (cond-> {:id (keyword index point-type)
                          :position (matrix/add offset pos)
                          :label label
                          :type :handle
                          :action :edit
                          :implied (boolean implied)
                          :rounded (boolean rounded)
                          :parent parent}
                   cursor
                   (assoc :cursor cursor)))))))

(m/=> acc-endpoints [:-> PathSegments [:vector Vec2]])
(defn acc-endpoints
  [segments]
  (reduce (fn [acc segment]
            (->> (peek acc)
                 (utils.path/abs-endpoint segment)
                 (conj acc))) [] segments))

(m/=> c->s-segment [:-> PathSegment PathSegment])
(defn c->s-segment
  [segment]
  (let [[_ _cp1x _cp1y cp2x cp2y x y] segment]
    #js ["S" cp2x cp2y x y]))

(m/=> s->c-segment [:-> PathSegments int? PathSegment])
(defn s->c-segment
  [segments index]
  (let [[_ cp2x cp2y x y] (aget segments index)
        prev-segment (aget segments (dec index))
        endpoints (acc-endpoints segments)
        prev-ep (get endpoints (dec index))
        [cp1x cp1y] (or (utils.path/outgoing-cp prev-segment) prev-ep)]
    #js ["C" cp1x cp1y cp2x cp2y x y]))

(m/=> toggle-shorthand [:-> PathSegments int? PathSegments])
(defn toggle-shorthand
  [segments index]
  (let [segment (aget segments index)
        command (utils.path/segment->command segment)
        new-seg (case command
                  "C" (c->s-segment segment)
                  "S" (s->c-segment segments index)
                  nil)]
    (if new-seg
      (doto (.slice segments) (aset index new-seg))
      segments)))

(defmethod element.hierarchy/handle-click :path
  [el handle]
  (let [point-type (keyword (name handle))
        index (js/parseInt (namespace handle))]
    (cond-> el
      (= point-type :end-point)
      (update-in [:attrs :d]
                 #(some-> %
                          (utils.path/string->segments)
                          (toggle-shorthand (inc index))
                          (utils.path/segments->string))))))

(defmethod element.hierarchy/handles :path
  [el]
  (let [segments (->> el :attrs :d utils.path/string->segments)
        endpoints (acc-endpoints segments)
        offset (utils.element/offset el)
        props {:parent (:id el)
               :endpoints endpoints
               :segments segments
               :offset offset}]
    (->> segments
         (map-indexed (partial segment-handles props))
         (flatten)
         (into []))))

(defmethod element.hierarchy/render-edit :path
  [el]
  (let [segments (->> el :attrs :d utils.path/string->segments)
        endpoints (acc-endpoints segments)
        offset (utils.element/offset el)
        props {:parent (:id el)
               :endpoints endpoints
               :segments segments
               :offset offset}]
    (->> segments
         (map-indexed (partial render-arms props))
         (into [:g]))))

(m/=> translate-seg-point [:->
                           PathSegments number? PathPointType Vec2
                           PathSegment])
(defn translate-seg-point
  [segments index point-type delta]
  (let [segment (aget segments index)
        [dx dy] delta
        cmd (utils.path/segment->command segment)]
    (case cmd
      "H"
      (if (= point-type :end-point)
        (let [new-seg (.slice segment)]
          (aset new-seg 1 (utils.length/transform (aget segment 1) + dx))
          (doto (.slice segments) (aset index new-seg)))
        segments)

      "V"
      (if (= point-type :end-point)
        (let [new-seg (.slice segment)]
          (aset new-seg 1 (utils.length/transform (aget segment 1) + dy))
          (doto (.slice segments) (aset index new-seg)))
        segments)

      (let [[xi yi] (utils.path/point-indices cmd point-type)]
        (if (and xi yi)
          (let [new-seg (.slice segment)]
            (aset new-seg xi (utils.length/transform (aget segment xi) + dx))
            (aset new-seg yi (utils.length/transform (aget segment yi) + dy))
            (doto (.slice segments) (aset index new-seg)))
          segments)))))

(m/=> translate-point [:-> int? PathPointType Vec2 PathSegments PathSegments])
(defn translate-point
  [index point-type delta segments]
  (let [segments (translate-seg-point segments index point-type delta)]
    (if (= point-type :end-point)
      (let [cmd (utils.path/segment->command (aget segments index))
            next-cmd (utils.path/segment->command (aget segments (inc index)))]
        (cond-> segments
          (= cmd "C")
          (translate-seg-point index :end-control-point delta)

          (= cmd "S")
          (translate-seg-point index :start-control-point delta)

          (not= next-cmd "S")
          (translate-seg-point (inc index) :start-control-point delta)))
      segments)))

(defn snap-control-point-to-angle
  [segments index point-type offset]
  (let [endpoints (acc-endpoints segments)
        cmd (utils.path/segment->command (aget segments index))
        anchor (if (and (= point-type :start-control-point) (not= cmd "S"))
                 (get endpoints (dec index))
                 (get endpoints index))
        cp-pos (->px-point (aget segments index) point-type)
        new-cp-pos (matrix/add cp-pos offset)
        snapped (input.handlers/snap-angle anchor new-cp-pos)]
    (matrix/sub snapped cp-pos)))

(defmethod element.hierarchy/handle-drag :path
  [el offset handle lock?]
  (let [point-type (keyword (name handle))
        index (js/parseInt (namespace handle))]
    (update-in el [:attrs :d]
               #(let [segments (utils.path/string->segments %)
                      snap-fn (if (= point-type :end-point)
                                input.handlers/lock-direction
                                (partial snap-control-point-to-angle
                                         segments index
                                         point-type))
                      offset (cond->> offset lock? snap-fn)]
                  (->> segments
                       (translate-point index point-type offset)
                       (utils.path/segments->string))))))

(defmethod element.hierarchy/delete-segments :path
  [el]
  (let [segments (-> el :attrs :d utils.path/string->segments)
        selected-indices (->> (:selected-handles el)
                              (keep namespace)
                              (map js/parseInt)
                              (remove zero?)
                              (into #{}))
        updated-segments (->> segments
                              (keep-indexed (fn [index segment]
                                              (when-not (contains?
                                                         selected-indices
                                                         index)
                                                segment)))
                              (into []))]
    (-> el
        (assoc :selected-handles #{})
        (assoc-in [:attrs :d] (utils.path/segments->string updated-segments)))))

(defmethod element.hierarchy/path :path
  [el]
  (-> el :attrs :d))
