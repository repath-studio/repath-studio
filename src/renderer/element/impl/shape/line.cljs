(ns renderer.element.impl.shape.line
  "https://www.w3.org/TR/SVG/shapes.html#LineElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/line"
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.hierarchy :as hierarchy]
   [renderer.input.handlers :as input.handlers]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]))

(hierarchy/derive! :line ::element.hierarchy/shape)

(defmethod element.hierarchy/properties :line
  []
  {:icon "line"
   :label [::label "Line"]
   :description [::description
                 "The <line> element is an SVG basic shape used to create a line
                  connecting two points."]
   :attrs [:stroke
           :stroke-width
           :stroke-linecap
           :stroke-dasharray
           :opacity]})

(defmethod element.hierarchy/translate :line
  [el [x y]]
  (-> el
      (attribute.hierarchy/update-attr :x1 + x)
      (attribute.hierarchy/update-attr :y1 + y)
      (attribute.hierarchy/update-attr :x2 + x)
      (attribute.hierarchy/update-attr :y2 + y)))

(defmethod element.hierarchy/scale :line
  [el ratio pivot-point]
  (let [{:keys [x1 y1 x2 y2]} (:attrs el)
        [x1 y1 x2 y2] (mapv utils.length/unit->px [x1 y1 x2 y2])
        dimensions (utils.bounds/->dimensions (element.hierarchy/bbox el))
        [x y] (matrix/sub dimensions (matrix/mul dimensions ratio))
        pivot-diff (matrix/sub pivot-point dimensions)
        offset (utils.element/scale-offset ratio pivot-diff)]
    (-> el
        (attribute.hierarchy/update-attr (if (< x1 x2) :x1 :x2) + x)
        (attribute.hierarchy/update-attr (if (< y1 y2) :y1 :y2) + y)
        (element.hierarchy/translate offset))))

(defmethod element.hierarchy/path :line
  [el]
  (let [{{:keys [x1 y1 x2 y2]} :attrs} el
        [x1 y1 x2 y2] (mapv utils.length/unit->px [x1 y1 x2 y2])]
    (string/join " " ["M" x1 y1
                      "L" x2 y2])))

(defmethod element.hierarchy/handles :line
  [el]
  (let [offset (utils.element/offset el)
        {{:keys [x1 y1 x2 y2]} :attrs} el
        [x1 y1 x2 y2] (mapv utils.length/unit->px [x1 y1 x2 y2])
        [x1 y1] (matrix/add offset [x1 y1])
        [x2 y2] (matrix/add offset [x2 y2])]
    [{:x x1
      :y y1
      :id :starting-point
      :label [::starting-point "starting point"]
      :type :handle
      :action :edit
      :element-id (:id el)}
     {:x x2
      :y y2
      :id :ending-point
      :label [::ending-point "ending point"]
      :type :handle
      :action :edit
      :element-id (:id el)}]))

(defmethod element.hierarchy/edit-drag :line
  [el offset handle lock?]
  (let [[x y] (cond-> offset
                lock?
                (input.handlers/lock-direction))]
    (case handle
      :starting-point
      (-> el
          (attribute.hierarchy/update-attr :x1 + x)
          (attribute.hierarchy/update-attr :y1 + y))

      :ending-point
      (-> el
          (attribute.hierarchy/update-attr :x2 + x)
          (attribute.hierarchy/update-attr :y2 + y))

      el)))

(defmethod element.hierarchy/bbox :line
  [el]
  (let [{{:keys [x1 y1 x2 y2]} :attrs} el
        [x1 y1 x2 y2] (mapv utils.length/unit->px [x1 y1 x2 y2])]
    [(min x1 x2) (min y1 y2) (max x1 x2) (max y1 y2)]))

#_(defmethod hierarchy/snapping-points :line
    [el]
    (let [{{:keys [x1 y1 x2 y2]} :attrs} el
          [x1 y1 x2 y2] (mapv utils.length/unit->px [x1 y1 x2 y2])]
      [(with-meta [x1 y1] {:label [::line-start "line start"]})
       (with-meta [x2 y2] {:label [::line-end "line end"]})]))
