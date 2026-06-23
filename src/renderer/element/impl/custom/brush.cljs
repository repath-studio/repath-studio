(ns renderer.element.impl.custom.brush
  "https://github.com/steveruizok/perfect-freehand"
  (:require
   ["perfect-freehand" :refer [getStroke]]
   [clojure.core.matrix :as matrix]
   [clojure.core.matrix.stats :as matrix.stats]
   [clojure.string :as string]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.impl.range :as attribute.impl.range]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.hierarchy :as hierarchy]
   [renderer.input.handlers :as input.handlers]
   [renderer.input.impl.pointer :as input.impl.pointer]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]
   [renderer.utils.path :as utils.path]))

(hierarchy/derive! :brush ::element.hierarchy/renderable)

(defmethod element.hierarchy/properties :brush
  []
  {:icon "brush"
   :label [::label "Brush"]
   :description [::description "Draw pressure-sensitive freehand lines using
                                perfect-freehand."]
   :url "https://github.com/steveruizok/perfect-freehand"
   :attrs [:points
           :fill
           :opacity
           :size
           :thinning
           :smoothing
           :streamline
           :id
           :class]})

(def option-keys
  [:size :thinning :smoothing :streamline])

(hierarchy/derive! :thinning ::attribute.impl.range/range)
(hierarchy/derive! :smoothing ::attribute.impl.range/range)
(hierarchy/derive! :streamline ::attribute.impl.range/range)

(defmethod attribute.hierarchy/form-element [:brush :size]
  [_ k v attrs]
  [attribute.views/range-input k v (merge attrs {:min 1
                                                 :max 100
                                                 :step 1})])

(defmethod attribute.hierarchy/form-element [:brush :points]
  [_ _k v]
  [:input.form-element {:value v
                        :disabled true
                        :placeholder (when-not v "multiple")}])

(defmethod attribute.hierarchy/description [:brush ::points]
  []
  [::points "Input points recorded from a user's mouse movement."])

(defmethod attribute.hierarchy/description [:brush :size]
  []
  [::size "The base size (diameter) of the stroke."])

(defmethod attribute.hierarchy/description [:brush :thinning]
  []
  [::thinning "The effect of pressure on the stroke's size."])

(defmethod attribute.hierarchy/description [:brush :smoothing]
  []
  [::smoothing "How much to soften the stroke's edges."])

(defmethod attribute.hierarchy/description [:brush :streamline]
  []
  [::stream-line "How much to streamline the stroke."])

(defn get-svg-path-from-stroke
  "Turns the points returned by getStroke into SVG path data.
   Ported from https://github.com/steveruizok/perfect-freehand#rendering"
  [points]
  (let [length (count points)]
    (if (< length 4)
      ""
      (let [[[ax ay] [bx by] [cx cy]] points
            d (str "M" (utils.length/->fixed ax) "," (utils.length/->fixed ay)
                   " Q" (utils.length/->fixed bx) "," (utils.length/->fixed by)
                   " " (utils.length/->fixed (matrix.stats/mean [bx cx])) ","
                   (utils.length/->fixed (matrix.stats/mean [by cy])) " T")]
        (reduce-kv
         (fn [result index]
           (if (or (= length (inc index))
                   (< index 2))
             result
             (let [[ax ay] (nth points index)
                   [bx by] (nth points (inc index))]
               (str result
                    (utils.length/->fixed (matrix.stats/mean [ax bx]))
                    ","
                    (utils.length/->fixed (matrix.stats/mean [ay by]))
                    " ")))) d points)))))

(def partition-to-px
  (comp
   (map utils.length/unit->px)
   (partition-all 3)))

(defn points->path
  [points options]
  (-> (into [] partition-to-px (utils.attribute/str->seq points))
      (clj->js)
      (getStroke (clj->js options))
      (js->clj)
      (get-svg-path-from-stroke)))

(defmethod element.hierarchy/render :brush
  [el]
  (let [attrs (:attrs el)
        pointer-handler (partial input.impl.pointer/handler! el)
        options (-> attrs
                    (select-keys option-keys)
                    (update-vals js/parseFloat))]
    [:path (merge {:d (points->path (:points attrs) options)
                   :on-pointer-up pointer-handler
                   :on-pointer-down pointer-handler
                   :on-pointer-move pointer-handler}
                  (select-keys attrs [:id :class :opacity :fill]))]))

(defn points->vec
  [points]
  (utils.attribute/points->vec points 3))

(defmethod element.hierarchy/bbox :brush
  [el]
  (let [points (-> el :attrs :points points->vec)
        min-x (apply min (map #(utils.length/unit->px (first %)) points))
        min-y (apply min (map #(utils.length/unit->px (second %)) points))
        max-x (apply max (map #(utils.length/unit->px (first %)) points))
        max-y (apply max (map #(utils.length/unit->px (second %)) points))]
    [min-x min-y max-x max-y]))

(defn translate
  [[offset-x offset-y] points point]
  (let [[point-x point-y pressure] point]
    (cond-> points
      point
      (conj (utils.length/transform point-x + offset-x)
            (utils.length/transform point-y + offset-y)
            pressure))))

(defmethod element.hierarchy/translate :brush
  [el offset]
  (update-in el
             [:attrs :points]
             #(->> (utils.attribute/str->seq %)
                   (transduce (partition-all 3) (partial translate offset) [])
                   (string/join " "))))

(defn- scale-el
  [ratio bbox-min offset el]
  (->> (utils.attribute/str->seq el)
       (into [] partition-to-px)
       (reduce (fn [points point]
                 (let [rel-point (matrix/sub bbox-min (take 2 point))
                       rel-offset (utils.element/scale-offset ratio rel-point)
                       offset (matrix/add offset rel-offset)]
                   (translate offset points point))) [])
       (string/join " ")))

(defmethod element.hierarchy/scale :brush
  [el ratio pivot-point]
  (let [bbox-min (take 2 (element.hierarchy/bbox el))
        offset (utils.element/scale-offset ratio pivot-point)]
    (update-in el [:attrs :points] (partial scale-el ratio bbox-min offset))))

(defmethod element.hierarchy/path :brush
  [el]
  (let [options (select-keys (:attrs el) option-keys)]
    (-> el :attrs :points
        (points->path options)
        (utils.path/manipulate nil))))

(defmethod element.hierarchy/handles :brush
  [el]
  (let [offset (utils.element/offset el)]
    (->> el :attrs :points
         (points->vec)
         (map-indexed (fn [index point]
                        (let [point (mapv utils.length/unit->px (take 2 point))]
                          {:id (keyword (str index))
                           :type :handle
                           :label [::brush-point "brush point"]
                           :action :edit
                           :parent (:id el)
                           :position (matrix/add offset point)})))
         (into []))))

(defmethod element.hierarchy/handle-drag :brush
  [el offset handle lock?]
  (let [index (js/parseInt (name handle))
        [x y] (cond-> offset
                lock?
                (input.handlers/lock-direction))
        transform-point (fn [[px py pressure]]
                          (vector (utils.length/transform px + x)
                                  (utils.length/transform py + y)
                                  pressure))]
    (update-in el [:attrs :points] #(-> (points->vec %)
                                        (update index transform-point)
                                        (flatten)
                                        (->> (string/join " ")
                                             (string/trim))))))
