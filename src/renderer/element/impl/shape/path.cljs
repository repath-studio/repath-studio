(ns renderer.element.impl.shape.path
  "https://www.w3.org/TR/SVG/paths.html#PathElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/path"
  (:require
   ["svg-path-bbox" :refer [svgPathBbox]]
   ["svgpath" :as svgpath]
   [clojure.core.matrix :as matrix]
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
  (update-in el [:attrs :d] #(-> (svgpath %)
                                 (.translate x y)
                                 (.toString))))

(defmethod element.hierarchy/scale :path
  [el ratio pivot-point]
  (let [[scale-x scale-y] ratio
        offset (utils.element/scale-offset ratio pivot-point)
        [x y] (element.hierarchy/bbox el)
        [x y] (-> (matrix/add [x y] offset)
                  (matrix/sub (matrix/mul ratio [x y])))]
    (update-in el [:attrs :d] #(-> (svgpath %)
                                   (.scale scale-x scale-y)
                                   (.translate x y)
                                   (.toString)))))

(defmethod element.hierarchy/bbox :path
  [el]
  (-> el :attrs :d svgPathBbox vec))

(defn ->px-point
  [seg point-type]
  (when-let [[xi yi] (utils.path/point-indices (aget seg 0) point-type)]
    (->> [(aget seg xi) (aget seg yi)]
         (mapv utils.length/unit->px))))

(defn render-arms
  [endpoints offset index seg]
  (let [prev-ep (some-> (get endpoints (dec index)) (matrix/add offset))
        cp0 (some-> seg (->px-point :start-control-point) (matrix/add offset))
        ep (some-> seg (->px-point :end-point) (matrix/add offset))]
    (case (utils.path/segment->cmd seg)
      "C"
      (let [cp1 (matrix/add (->px-point seg :end-control-point) offset)]
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

(defn handles
  [endpoints index seg]
  (let [cmd (utils.path/segment->cmd seg)
        prev-ep (get endpoints (dec index))]
    (case cmd
      ("M" "L" "T" "A")
      [{:point-type :end-point
        :pos (->px-point seg :end-point)}]

      "C"
      [(when prev-ep
         {:point-type :start-control-point
          :pos (->px-point seg :start-control-point)
          :rounded true})
       {:point-type :end-control-point
        :pos (->px-point seg :end-control-point)
        :rounded true}
       {:point-type :end-point
        :pos (->px-point seg :end-point)}]

      ("S" "Q")
      [{:point-type :start-control-point
        :pos (->px-point seg :start-control-point)
        :rounded true}
       {:point-type :end-point
        :pos (->px-point seg :end-point)}]

      "H"
      [(when-let [[_ prev-y] prev-ep]
         {:point-type :end-point
          :pos (->> [(aget seg 1) prev-y]
                    (mapv utils.length/unit->px))})]

      "V"
      [(when-let [[prev-x _] prev-ep]
         {:point-type :end-point
          :pos (->> [prev-x (aget seg 1)]
                    (mapv utils.length/unit->px))})]

      nil)))

(defn render-handles
  [{:keys [element-id endpoints offset]} index seg]
  (->> (handles endpoints index seg)
       (map (fn [{:keys [point-type pos rounded]}]
              (let [[ax ay] (matrix/add offset pos)]
                [tool.views/handle {:id (keyword index point-type)
                                    :x ax
                                    :y ay
                                    :type :handle
                                    :action :edit
                                    :rounded rounded
                                    :element-id element-id}])))
       (into [:g])))

(defn acc-endpoints
  [segments]
  (->> (range (.-length segments))
       (reduce
        (fn [acc i]
          (conj acc (utils.path/abs-endpoint (aget segments i) (peek acc))))
        [])))

(defmethod element.hierarchy/render-edit :path
  [el]
  (let [offset (utils.element/offset el)
        segments (-> el :attrs :d svgpath .abs .-segments)
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

(defn translate-seg-point!
  [segments index point-type [dx dy]]
  (when-let [seg (aget segments index)]
    (let [cmd (utils.path/segment->cmd seg)]
      (case cmd
        "H"
        (when (= point-type :end-point)
          (let [new-seg (.slice seg)]
            (aset new-seg 1 (utils.length/transform (aget seg 1) + dx))
            (aset segments index new-seg)))

        "V"
        (when (= point-type :end-point)
          (let [new-seg (.slice seg)]
            (aset new-seg 1 (utils.length/transform (aget seg 1) + dy))
            (aset segments index new-seg)))

        (when-let [[xi yi] (utils.path/point-indices cmd point-type)]
          (let [new-seg (.slice seg)]
            (aset new-seg xi (utils.length/transform (aget seg xi) + dx))
            (aset new-seg yi (utils.length/transform (aget seg yi) + dy))
            (aset segments index new-seg)))))))

(defn translate-point
  [svgpath-obj index point-type delta]
  (let [segments (.-segments svgpath-obj)]
    (translate-seg-point! segments index point-type delta)
    (when (= point-type :end-point)
      (translate-seg-point! segments index :end-control-point delta)
      (translate-seg-point! segments (inc index) :start-control-point delta)))
  svgpath-obj)

(defmethod element.hierarchy/edit :path
  [el offset handle lock?]
  (let [offset (cond-> offset lock? input.handlers/lock-direction)
        index (js/parseInt (namespace handle))
        point-type (keyword (name handle))]
    (update-in el [:attrs :d]
               #(-> (svgpath %)
                    (.abs)
                    (translate-point index point-type offset)
                    (.toString)))))

(defmethod element.hierarchy/path :path
  [el]
  (-> el :attrs :d))
