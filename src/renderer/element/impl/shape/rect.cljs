(ns renderer.element.impl.shape.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/rect"
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.hierarchy :as hierarchy]
   [renderer.input.handlers :as input.handlers]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]
   [renderer.utils.math :as utils.math]))

(hierarchy/derive! :rect ::element.hierarchy/box)
(hierarchy/derive! :rect ::element.hierarchy/shape)

(defmethod element.hierarchy/properties :rect
  []
  {:icon "rectangle"
   :label [::label "Rectangle"]
   :description [::description
                 "The <rect> element is a basic SVG shape that draws
                  rectangles, defined by their position, width, and height.
                  The rectangles may have their corners rounded."]
   :attrs [:stroke-width
           :opacity
           :fill
           :stroke
           :stroke-dasharray
           :stroke-linejoin]})

(defmethod element.hierarchy/scale :rect
  [el ratio pivot-point]
  (let [[x-ratio y-ratio] ratio
        {{:keys [width height rx ry]} :attrs} el
        [w h] (mapv utils.length/unit->px [width height])
        [offset-x offset-y] (utils.element/scale-offset ratio pivot-point)
        offset [(+ offset-x (min 0 (* w x-ratio)))
                (+ offset-y (min 0 (* h y-ratio)))]]
    (cond-> el
      :always
      (-> (attribute.hierarchy/update-attr :width #(abs (* % x-ratio)))
          (attribute.hierarchy/update-attr :height #(abs (* % y-ratio)))
          (element.hierarchy/translate offset))

      rx
      (attribute.hierarchy/update-attr :rx #(abs (* % x-ratio)))

      ry
      (attribute.hierarchy/update-attr :ry #(abs (* % y-ratio))))))

(defn clamp-radius-to-size
  [el]
  (let [{{:keys [rx ry width height]} :attrs} el
        [w h] (mapv utils.length/unit->px [width height])]
    (cond-> el
      rx (attribute.hierarchy/update-attr :rx min (/ w 2))
      ry (attribute.hierarchy/update-attr :ry min (/ h 2)))))

(defmethod element.hierarchy/handle-drag :rect
  [el offset handle lock?]
  (let [[x y] (cond-> offset
                (and (contains? #{:position :size} handle)
                     lock?)
                (input.handlers/lock-direction))
        [w h] (utils.bounds/->dimensions (:bbox el))
        clamp-radius (fn [r max-size]
                       (min (max 0 r)
                            (/ (if lock?
                                 (min w h)
                                 max-size) 2)))]
    (case handle
      :position
      (-> el
          (attribute.hierarchy/update-attr :width (comp (partial max 0) -) x)
          (attribute.hierarchy/update-attr :height (comp (partial max 0) -) y)
          (element.hierarchy/translate [x y])
          (clamp-radius-to-size))

      :size
      (-> el
          (attribute.hierarchy/update-attr :width (comp (partial max 0) +) x)
          (attribute.hierarchy/update-attr :height (comp (partial max 0) +) y)
          (clamp-radius-to-size))

      :rx
      (cond-> el
        :always
        (attribute.hierarchy/update-attr :rx (comp #(clamp-radius % w) -) x)

        lock?
        (update :attrs (fn [attrs] (assoc attrs :ry (:rx attrs)))))

      :ry
      (cond-> el
        :always
        (attribute.hierarchy/update-attr :ry (comp #(clamp-radius % h) +) y)

        lock?
        (update :attrs (fn [attrs] (assoc attrs :rx (:ry attrs)))))

      el)))

(defmethod element.hierarchy/handles :rect
  [el]
  (let [[min-x min-y max-x max-y] (:bbox el)
        {{:keys [rx ry]} :attrs} el
        [rx ry] (mapv utils.length/unit->px [rx ry])]
    [{:type :handle
      :action :edit
      :parent (:id el)
      :position [min-x min-y]
      :id :position
      :label [::position-handle "position handle"]}
     {:type :handle
      :action :edit
      :parent (:id el)
      :position [max-x max-y]
      :id :size
      :label [::size-handle "size handle"]}
     {:type :handle
      :action :edit
      :rounded true
      :parent (:id el)
      :position [(- max-x rx) min-y]
      :id :rx
      :label [::rx-handle "x radius handle"]}
     {:type :handle
      :action :edit
      :rounded true
      :parent (:id el)
      :position [max-x (+ min-y ry)]
      :id :ry
      :label [::ry-handle "y radius handle"]}]))

(defmethod element.hierarchy/path :rect
  [el]
  (let [{{:keys [x y width height rx ry]} :attrs} el
        [x y width height] (mapv utils.length/unit->px [x y width height])
        rx (utils.length/unit->px (if (and (not rx) ry) ry rx))
        ry (utils.length/unit->px (if (and (not ry) rx) rx ry))
        rx (if (> rx (/ width 2)) (/ width 2) rx)
        ry (if (> ry (/ height 2)) (/ height 2) ry)
        curved? (and (> rx 0) (> ry 0))
        y2-full (+ y height)
        x2-full (+ x width)
        [x1 y1] (matrix/add [x y] [rx ry])
        [x2 y2] (matrix/sub [x2-full y2-full] [rx ry])
        [krx kry] (matrix/mul [rx ry] utils.math/KAPPA)]
    (cond-> []
      :always (conj "M" x1 y
                    "L" x2 y)
      curved? (conj "C" (+ x2 krx) y x2-full (- y1 kry) x2-full y1)
      :always (conj "L" x2-full y2)
      curved? (conj "C" x2-full (+ y2 kry) (+ x2 krx) y2-full x2 y2-full)
      :always (conj "L" x1 y2-full)
      curved? (conj "C" (- x1 krx) y2-full x (+ y2 kry) x y2)
      curved? (conj "L" x y1)
      curved? (conj "C" x (- y1 kry) (- x1 krx) y x1 y)
      :always (conj "z")
      :always (->> (map #(cond-> % (number? %) utils.length/->fixed))
                   (string/join " ")))))
