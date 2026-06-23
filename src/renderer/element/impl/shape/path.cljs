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

(defn update-path
  [el f]
  (update-in el [:attrs :d] #(some-> (svgpath %)
                                     (f)
                                     (.round 3)
                                     (.toString))))

(defmethod element.hierarchy/translate :path
  [el [x y]]
  (update-path el #(.translate % x y)))

(defmethod element.hierarchy/scale :path
  [el ratio pivot-point]
  (let [[scale-x scale-y] ratio
        offset (utils.element/scale-offset ratio pivot-point)
        [x y] (element.hierarchy/bbox el)
        [x y] (-> (matrix/add [x y] offset)
                  (matrix/sub (matrix/mul ratio [x y])))]
    (update-path el #(-> %
                         (.scale scale-x scale-y)
                         (.translate x y)))))

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
  (let [prev-ep (some-> endpoints (get (dec index)) (matrix/add offset))
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
                                      (utils.path/outgoing-cp)
                                      (matrix/add offset))]
         [utils.svg/arm prev-ep implied-cp1])
       [utils.svg/arm cp0 ep]]

      "Q"
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
  [{:keys [parent endpoints segments offset cp-indices]} index segment]
  (->> (handles endpoints segments index segment)
       (keep (fn [{:keys [point-type pos rounded implied cursor]}]
               (when (or (= point-type :end-point)
                         (contains? cp-indices index))
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
                     (assoc :cursor cursor))))))
       (into [])))

(m/=> cycle-segment [:-> PathSegments int? PathSegments])
(defn cycle-segment
  [segments index]
  (let [command (utils.path/segment->command (aget segments index))
        next-cmd (case command
                   ("H" "V") "L"
                   ("L" "T") "Q"
                   "Q" "C"
                   "C" "S"
                   "S" "L"
                   nil)]
    (cond-> segments
      next-cmd (utils.path/convert-segment index next-cmd))))

(defmethod element.hierarchy/handle-click :path
  [el handle]
  (let [point-type (keyword (name handle))
        index (js/parseInt (namespace handle))]
    (cond-> el
      (= point-type :end-point)
      (update-in [:attrs :d]
                 #(some-> %
                          (utils.path/string->segments)
                          (cycle-segment index)
                          (utils.path/segments->string))))))

(defn segment-props
  [el segments]
  (let [endpoints (utils.path/acc-endpoints segments)
        offset (utils.element/offset el)
        selected (->> (:selected-handles el)
                      (keep #(some-> (namespace %) js/parseInt))
                      (into #{}))
        cp-indices (into #{} (mapcat (fn [i] [i (inc i)]) selected))]
    {:parent (:id el)
     :endpoints endpoints
     :segments segments
     :offset offset
     :cp-indices cp-indices}))

(defmethod element.hierarchy/handles :path
  [el]
  (let [segments (->> el :attrs :d utils.path/string->segments)
        props (segment-props el segments)]
    (->> segments
         (map-indexed (partial segment-handles props))
         (flatten)
         (into []))))

(defmethod element.hierarchy/render-edit :path
  [el]
  (let [segments (->> el :attrs :d utils.path/string->segments)
        props (segment-props el segments)]
    (->> segments
         (map-indexed (fn [index segment]
                        (when (contains? (:cp-indices props) index)
                          (render-arms props index segment))))
         (into [:g]))))

(m/=> translate-point [:-> PathSegments number? Vec2 PathPointType PathSegment])
(defn translate-point
  [index delta point-type segments]
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

(m/=> translate-controls [:->
                          int? Vec2 [:set keyword?] PathSegments
                          PathSegments])
(defn translate-controls
  [index delta selected-handles segments]
  (let [cmd (utils.path/segment->command (aget segments index))
        next-cmd (utils.path/segment->command (aget segments (inc index)))
        movable? (fn [index point-type]
                   (not (contains? selected-handles
                                   (keyword index point-type))))]
    (cond->> segments
      (and (= cmd "C") (movable? index "end-control-point"))
      (translate-point index delta :end-control-point)

      (and (= cmd "Q")
           (movable? index "start-control-point")
           (movable? (dec index) "end-point"))
      (translate-point index delta :start-control-point)

      (and (= cmd "S") (movable? index "start-control-point"))
      (translate-point index delta :start-control-point)

      (and (not= next-cmd "S") (movable? (inc index) "start-control-point"))
      (translate-point (inc index) delta :start-control-point))))

(defn snap-control-point-to-angle
  [segments index point-type offset]
  (let [endpoints (utils.path/acc-endpoints segments)
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
                  (cond->> segments
                    :always
                    (translate-point index offset point-type)

                    (= point-type :end-point)
                    (translate-controls index offset (:selected-handles el))

                    :always
                    (utils.path/segments->string))))))

(defmethod element.hierarchy/delete-segments :path
  [el]
  (let [segments (-> el :attrs :d utils.path/string->segments)
        selected-indices (->> (:selected-handles el)
                              (keep namespace)
                              (map js/parseInt)
                              (into #{}))
        endpoints (utils.path/acc-endpoints segments)
        first-remaining-idx (->> (range (count segments))
                                 (remove selected-indices)
                                 (first))
        updated-segments (->> segments
                              (keep-indexed (fn [index segment]
                                              (when-not (contains?
                                                         selected-indices
                                                         index)
                                                segment)))
                              (into-array))
        updated-segments (if (and (seq updated-segments)
                                  first-remaining-idx
                                  (not= "M" (utils.path/segment->command
                                             (aget updated-segments 0))))
                           (let [[ex ey] (get endpoints first-remaining-idx)
                                 second-cmd (utils.path/segment->command
                                             (aget updated-segments 1))
                                 resolved (case second-cmd
                                            "S" (utils.path/convert-segment
                                                 updated-segments 1 "C")
                                            "T" (utils.path/convert-segment
                                                 updated-segments 1 "Q")
                                            updated-segments)]
                             (doto resolved (aset 0 #js ["M" ex ey])))
                           updated-segments)]
    (-> el
        (assoc :selected-handles #{})
        (assoc-in [:attrs :d] (utils.path/segments->string updated-segments)))))

(defmethod element.hierarchy/path :path
  [el]
  (-> el :attrs :d))
