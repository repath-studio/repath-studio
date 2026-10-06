(ns renderer.element.impl.shape.circle
  "https://www.w3.org/TR/SVG/shapes.html#CircleElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/circle"
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.hierarchy :as hierarchy]
   [renderer.tool.impl.element.core :as-alias element.core]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]
   [renderer.utils.math :as utils.math]
   [renderer.utils.svg :as utils.svg]))

(hierarchy/derive! :circle ::element.hierarchy/shape)

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
           :stroke
           :stroke-dasharray]})

(defmethod element.hierarchy/translate :circle
  [el [x y]]
  (-> el
      (attribute.hierarchy/update-attr :cx + x)
      (attribute.hierarchy/update-attr :cy + y)))

(defmethod element.hierarchy/scale :circle
  [el ratio pivot-point]
  (let [{{:keys [stroke-width]} :attrs} el
        padding (/ (utils.length/unit->px stroke-width) 2)
        dimensions (-> el element.hierarchy/bbox utils.bounds/->dimensions)
        pivot-point (->> (matrix/div dimensions 2)
                         (matrix/sub pivot-point))
        offset (utils.element/scale-offset ratio pivot-point)
        ratio (apply min ratio)]
    (-> el
        (attribute.hierarchy/update-attr :r #(- (* (+ % padding) (abs ratio))
                                                padding))
        (element.hierarchy/translate offset))))

(defmethod element.hierarchy/bbox :circle
  [el]
  (let [{{:keys [cx cy r stroke-width]} :attrs} el
        [cx cy r stroke-width] (->> [cx cy r stroke-width]
                                    (map utils.length/unit->px))
        padding (/ stroke-width 2)]
    [(- cx r padding) (- cy r padding) (+ cx r padding) (+ cy r padding)]))

(defmethod element.hierarchy/area :circle
  [el]
  (-> (get-in el [:attrs :r])
      (utils.length/unit->px)
      (Math/pow 2)
      (* Math/PI)))

(defmethod element.hierarchy/path :circle
  [el]
  (let [{{:keys [cx cy r]} :attrs} el
        [cx cy r] (map utils.length/unit->px [cx cy r])
        kr (* utils.math/KAPPA r)]
    (->> ["M" (+ cx r) cy
          "C" (+ cx r) (+ cy kr) (+ cx kr) (+ cy r) cx (+ cy r)
          "S" (- cx r) (+ cy kr) (- cx r) cy
          "S" (- cx kr) (- cy r) cx (- cy r)
          "S" (+ cx r) (- cy kr) (+ cx r) cy
          "z"]
         (map #(cond-> % (number? %) utils.attribute/->fixed))
         (string/join " "))))

(defmethod element.hierarchy/handle-drag :circle
  [el [x _y] handle _lock?]
  (case handle
    :r (attribute.hierarchy/update-attr el :r #(abs (+ % x)))
    el))

(defmethod element.hierarchy/handles :circle
  [el]
  (let [{{:keys [cx cy r]} :attrs} el
        [cx cy r] (map utils.length/unit->px [cx cy r])]
    [{:position [(+ cx r) cy]
      :id :r
      :label [::r-handle "radius handle"]
      :cursor "ew-resize"
      :type :handle
      :action :edit
      :parent (:id el)}]))

(defmethod element.hierarchy/render-edit :circle
  [el]
  (let [transform @(rf/subscribe [::element.subs/transform (:id el)])
        {{:keys [cx cy r]} :attrs} el
        [cx cy r] (map utils.length/unit->px [cx cy r])
        wpoint (partial utils.element/transform-point transform)
        center (wpoint [cx cy])
        edge (wpoint [(+ cx r) cy])
        [label-x label-y] (wpoint [(+ cx (/ r 2)) cy])]
    [:g
     [utils.svg/line center edge :stroke "var(--accent-foreground)"]
     [utils.svg/line center edge :stroke-dasharray 5]
     [utils.svg/label (utils.attribute/->fixed r 2 false) {:x label-x
                                                           :y label-y}]
     [utils.svg/times center]]))

(defmethod element.hierarchy/snapping-points :circle
  [el]
  (let [{{:keys [cx cy r]} :attrs} el
        [cx cy r] (mapv utils.length/unit->px [cx cy r])]
    (mapv #(with-meta % {:label [::element.core/edge "edge"]})
          [[(- cx r) cy]
           [(+ cx r) cy]
           [cx (- cy r)]
           [cx (+ cy r)]])))
