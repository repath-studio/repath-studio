(ns renderer.element.impl.shape.ellipse
  "https://www.w3.org/TR/SVG/shapes.html#EllipseElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/ellipse"
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

(derive :ellipse ::element.hierarchy/shape)

(defmethod element.hierarchy/properties :ellipse
  []
  {:icon "ellipse"
   :label [::label "Ellipse"]
   :description [::description
                 "The <ellipse> element is an SVG basic shape, used to create
                  ellipses based on a center coordinate, and both their x and
                  y radius."]
   :attrs [:stroke-width
           :opacity
           :fill
           :stroke
           :stroke-dasharray]})

(defmethod element.hierarchy/translate :ellipse
  [el [x y]]
  (-> el
      (attribute.hierarchy/update-attr :cx + x)
      (attribute.hierarchy/update-attr :cy + y)))

(defmethod element.hierarchy/scale :ellipse
  [el ratio pivot-point]
  (let [[x y] ratio
        dimensions (-> el element.hierarchy/bbox utils.bounds/->dimensions)
        pivot-point (->> (matrix/div dimensions 2)
                         (matrix/sub pivot-point))
        offset (utils.element/scale-offset ratio pivot-point)]
    (-> el
        (attribute.hierarchy/update-attr :rx * x)
        (attribute.hierarchy/update-attr :ry * y)
        (element.hierarchy/translate offset))))

(defmethod element.hierarchy/bbox :ellipse
  [el]
  (let [{{:keys [cx cy rx ry]} :attrs} el
        rx (or rx ry)
        ry (or ry rx)
        [cx cy rx ry] (map utils.length/unit->px [cx cy rx ry])]
    [(- cx rx) (- cy ry) (+ cx rx) (+ cy ry)]))

(defmethod element.hierarchy/path :ellipse
  [el]
  (let [{{:keys [cx cy rx ry]} :attrs} el
        rx (or rx ry)
        ry (or ry rx)
        [cx cy rx ry] (mapv utils.length/unit->px [cx cy rx ry])]
    (string/join " " ["M" (+ cx rx) cy
                      "A" rx ry 0 0 1 cx (+ cy ry)
                      "A" rx ry 0 0 1 (- cx rx) cy
                      "A" rx ry 0 0 1 (+ cx rx) cy
                      "z"])))

(defmethod element.hierarchy/area :ellipse
  [el]
  (let [{{:keys [rx ry]} :attrs} el
        rx (or rx ry)
        ry (or ry rx)
        [rx ry] (map utils.length/unit->px [rx ry])]
    (* Math/PI rx ry)))

(defmethod element.hierarchy/edit :ellipse
  [el [x y] handle _e]
  (let [{{:keys [rx ry]} :attrs} el]
    (case handle
      :rx (attribute.hierarchy/update-attr el (if rx :rx :ry) #(abs (+ % x)))
      :ry (attribute.hierarchy/update-attr el (if ry :ry :rx) #(abs (- % y)))
      el)))

(defmethod element.hierarchy/render-edit :ellipse
  [el]
  (let [bbox (:bbox el)
        [cx cy] (utils.bounds/center bbox)
        [rx ry] (matrix/div (utils.bounds/->dimensions bbox) 2)
        line-end-x (+ cx rx)
        line-end-y (- cy ry)]
    [:g ::edit-handles
     [utils.svg/times [cx cy]]

     [utils.svg/line [cx cy] [line-end-x cy] :stroke "var(--accent-foreground)"]
     [utils.svg/line [cx cy] [cx line-end-y] :stroke "var(--accent-foreground)"]

     [utils.svg/line [cx cy] [line-end-x cy] :stroke-dasharray 5]
     [utils.svg/line [cx cy] [cx line-end-y] :stroke-dasharray 5]

     [utils.svg/label (utils.length/->fixed rx 2 false) {:x (+ cx (/ rx 2))
                                                         :y cy}]

     [utils.svg/label (utils.length/->fixed ry 2 false) {:x cx
                                                         :y (- cy (/ ry 2))}]
     (map (fn [handle]
            ^{:key (:id handle)}
            [tool.views/square-handle
             (merge handle {:type :handle
                            :action :edit
                            :element-id (:id el)})])
          [{:x (+ cx rx)
            :y cy
            :id :rx
            :label [::rx-handle "x radius handle"]}
           {:x cx
            :y (- cy ry)
            :id :ry
            :label [::ry-handle "y radius handle"]}])]))
