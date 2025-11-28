(ns renderer.element.impl.shape.circle
  "https://www.w3.org/TR/SVG/shapes.html#CircleElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/circle"
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.tool.views :as tool.views]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]
   [renderer.utils.svg :as utils.svg]))

(derive :circle ::element.hierarchy/shape)

(defmethod element.hierarchy/properties :circle
  []
  {:icon "circle"
   :label [::label "Circle"]
   :description [::description
                 "The <circle> SVG element is an SVG basic shape, used to
                  draw circles based on a center point and a radius."]
   :ratio-locked true
   :attrs [:stroke-width
           :opacity
           :fill
           :stroke
           :stroke-dasharray]})

(defmethod element.hierarchy/translate :circle
  [el [x y]]
  (-> el
      (attribute.hierarchy/update-attr :cx + x)
      (attribute.hierarchy/update-attr :cy + y)))

(defmethod element.hierarchy/scale :circle
  [el ratio pivot-point]
  (let [dimensions (-> el element.hierarchy/bbox utils.bounds/->dimensions)
        pivot-point (->> (matrix/div dimensions 2)
                         (matrix/sub pivot-point))
        offset (utils.element/scale-offset ratio pivot-point)
        ratio (apply min ratio)]
    (-> el
        (attribute.hierarchy/update-attr :r * ratio)
        (element.hierarchy/translate offset))))

(defmethod element.hierarchy/bbox :circle
  [el]
  (let [{{:keys [cx cy r]} :attrs} el
        [cx cy r] (map utils.length/unit->px [cx cy r])]
    [(- cx r) (- cy r) (+ cx r) (+ cy r)]))

(defmethod element.hierarchy/area :circle
  [el]
  (-> (get-in el [:attrs :r])
      (utils.length/unit->px)
      (Math/pow 2)
      (* Math/PI)))

(defmethod element.hierarchy/path :circle
  [el]
  (let [{{:keys [cx cy r]} :attrs} el
        [cx cy r] (map utils.length/unit->px [cx cy r])]
    (string/join " " ["M" (+ cx r) cy
                      "A" r r 0 0 1 cx (+ cy r)
                      "A" r r 0 0 1 (- cx r) cy
                      "A" r r 0 0 1 (+ cx r) cy
                      "z"])))

(defmethod element.hierarchy/edit :circle
  [el [x _y] handle _e]
  (case handle
    :r (attribute.hierarchy/update-attr el :r #(abs (+ % x)))
    el))

(defmethod element.hierarchy/render-edit :circle
  [el]
  (let [bbox (:bbox el)
        [cx cy] (utils.bounds/center bbox)
        r (/ (first (utils.bounds/->dimensions bbox)) 2)]
    [:g
     [utils.svg/line [cx cy] [(+ cx r) cy]]
     [utils.svg/label (utils.length/->fixed r 2 false) {:x (+ cx (/ r 2))
                                                        :y cy}]
     [utils.svg/times [cx cy]]
     [tool.views/square-handle {:x (+ cx r)
                                :y cy
                                :id :r
                                :label [::r-handle "radius handle"]
                                :type :handle
                                :action :edit
                                :element-id (:id el)}
      [:title {:key "r-title"} "r"]]]))
