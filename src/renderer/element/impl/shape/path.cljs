(ns renderer.element.impl.shape.path
  "https://www.w3.org/TR/SVG/paths.html#PathElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/path"
  (:require
   ["svg-path-bbox" :refer [svgPathBbox]]
   ["svgpath" :as svgpath]
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.db :refer [PathSegment PathSegments PathPointType Vec2]]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.hierarchy :as hierarchy]
   [renderer.input.handlers :as input.handlers]
   [renderer.tool.subs :as-alias tool.subs]
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
       (when-let [implied-cp1 (some-> (get segments (dec index))
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
      [(when prev-ep
         {:point-type :start-control-point
          :pos (->px-point segment :start-control-point)
          :rounded true})
       {:point-type :end-control-point
        :pos (->px-point segment :end-control-point)
        :rounded true}
       {:point-type :end-point
        :pos (->px-point segment :end-point)}]

      "S"
      (let [cp0 (->px-point segment :start-control-point)
            implied-cp1 (some-> (get segments (dec index))
                                utils.path/outgoing-cp
                                (->> (mapv utils.length/unit->px)))]
        (cond-> [{:point-type :start-control-point
                  :pos cp0
                  :rounded true}
                 {:point-type :end-point
                  :pos (->px-point segment :end-point)}]
          implied-cp1 (conj {:point-type :implied-control-point
                             :pos implied-cp1
                             :rounded true
                             :implied true})))

      "Q"
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
  [{:keys [element-id endpoints segments offset]} index segment]
  (->> (handles endpoints segments index segment)
       (map (fn [{:keys [point-type pos rounded implied]}]
              (let [[ax ay] (matrix/add offset pos)
                    h [tool.views/handle {:id (keyword index point-type)
                                          :x ax
                                          :y ay
                                          :type :handle
                                          :action :edit
                                          :rounded (boolean rounded)
                                          :element-id element-id}]]
                (if implied
                  [:g {:pointer-events "none"
                       :opacity 0.5} h]
                  h))))
       (into [:g])))

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
    ["S" cp2x cp2y x y]))

(m/=> s->c-segment [:-> PathSegments int? PathSegment])
(defn s->c-segment
  [segments index]
  (let [[_ cp2x cp2y x y] (get segments index)
        prev-segment (get segments (dec index))
        endpoints (acc-endpoints segments)
        prev-ep (get endpoints (dec index))
        [cp1x cp1y] (or (utils.path/outgoing-cp prev-segment) prev-ep)]
    ["C" cp1x cp1y cp2x cp2y x y]))

(m/=> toggle-command [:-> PathSegments int? PathSegments])
(defn toggle-command
  [segments index]
  (let [segment (get segments index)
        command (utils.path/segment->command segment)]
    (case command
      "C" (assoc segments index (c->s-segment segment))
      "S" (assoc segments index (s->c-segment segments index))
      segments)))

(defmethod element.hierarchy/edit-click :path
  [el handle]
  (let [point-type (keyword (name handle))
        index (js/parseInt (namespace handle))]
    (cond-> el
      (= point-type :end-point)
      (update-in [:attrs :d]
                 #(some-> %
                          (utils.path/string->segments)
                          (toggle-command (inc index))
                          (utils.path/segments->string))))))

(m/=> ->highlight-segments [:->
                            PathSegments [:vector Vec2] Vec2 int?
                            [:maybe PathSegments]])
(defn ->highlight-segments
  [segments endpoints offset index]
  (let [segment (get segments index)
        cmd (utils.path/segment->command segment)
        prev-ep (some->> (dec index)
                         (get endpoints)
                         (mapv utils.length/unit->px)
                         (matrix/add offset))
        ep (some-> segment
                   (->px-point :end-point)
                   (matrix/add offset))]
    (when (and prev-ep ep)
      (case cmd
        "C"
        (let [cp1 (matrix/add (->px-point segment :start-control-point) offset)
              cp2 (matrix/add (->px-point segment :end-control-point) offset)]
          [(concat ["M"] prev-ep) (concat ["C"] cp1 cp2 ep)])

        "S"
        (let [cp1 (or (some->> (dec index)
                               (get segments)
                               (utils.path/outgoing-cp)
                               (mapv utils.length/unit->px)
                               (matrix/add offset))
                      prev-ep)
              cp2 (matrix/add (->px-point segment :start-control-point) offset)]
          [(concat ["M"] prev-ep) (concat ["C"] cp1 cp2 ep)])

        "Q"
        (let [cp (matrix/add (->px-point segment :start-control-point) offset)]
          [(concat ["M"] prev-ep) ["Q"] cp ep])

        [(concat ["M"] prev-ep) (concat ["L"] ep)]))))

(defn active-segment-indices
  [segments index point-type]
  (let [next-cmd (utils.path/segment->command (get segments (inc index)))]
    (cond-> #{index}
      (and (= point-type :end-point)
           (get segments (inc index)))
      (conj (inc index))

      (and (= point-type :end-control-point)
           (= next-cmd "S"))
      (conj (inc index)))))

(defn editing-segments-path
  [segments endpoints offset indices]
  (->> indices
       (mapv (partial ->highlight-segments segments endpoints offset))
       utils.path/segments->string))

(defmethod element.hierarchy/render-edit :path
  [el]
  (let [editing? @(rf/subscribe [::tool.subs/editing?])
        clicked-element @(rf/subscribe [::app.subs/clicked-element])
        zoom @(rf/subscribe [::document.subs/zoom])
        segments (->> el :attrs :d utils.path/string->segments)
        endpoints (acc-endpoints segments)
        offset (utils.element/offset el)
        props {:element-id (:id el)
               :endpoints endpoints
               :segments segments
               :offset offset}
        active-index (when (and editing?
                                (= (:element-id clicked-element) (:id el)))
                       (some-> clicked-element :id namespace js/parseInt))
        active-pt (when active-index (keyword (name (:id clicked-element))))
        active-indices (active-segment-indices segments active-index active-pt)
        segment-d (when active-index (editing-segments-path segments endpoints
                                                            offset
                                                            active-indices))]
    [:g
     (when segment-d
       [:path {:d segment-d
               :fill "transparent"
               :stroke-width (/ 1 zoom)
               :stroke "var(--accent)"}])
     (->> segments
          (map-indexed (partial render-arms props))
          (into [:g]))
     (->> segments
          (map-indexed (partial render-handles props))
          (into [:g]))]))

(m/=> translate-seg-point [:->
                           PathSegments number? PathPointType Vec2
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
    (if (= point-type :end-point)
      (let [cmd (utils.path/segment->command (get segments index))
            next-cmd (utils.path/segment->command (get segments (inc index)))]
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
        cmd (utils.path/segment->command (get segments index))
        anchor (if (and (= point-type :start-control-point) (not= cmd "S"))
                 (get endpoints (dec index))
                 (get endpoints index))
        cp-pos (->px-point (get segments index) point-type)
        new-cp-pos (matrix/add cp-pos offset)
        snapped (input.handlers/snap-angle anchor new-cp-pos)]
    (matrix/sub snapped cp-pos)))

(defmethod element.hierarchy/edit-drag :path
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

(defmethod element.hierarchy/path :path
  [el]
  (-> el :attrs :d))
