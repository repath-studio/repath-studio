(ns renderer.element.impl.shape.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/rect"
  (:require
   [clojure.string :as string]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.impl.box :as element.impl.box]
   [renderer.event.handlers :as event.handlers]
   [renderer.tool.views :as tool.views]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]))

(element.hierarchy/derive-element :rect ::element.hierarchy/box)
(element.hierarchy/derive-element :rect ::element.hierarchy/shape)

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
  (let [[x y] ratio
        offset (utils.element/scale-offset ratio pivot-point)]
    (cond-> el
      :always
      (-> (attribute.hierarchy/update-attr :width * x)
          (attribute.hierarchy/update-attr :height * y)
          (element.hierarchy/translate offset))

      (-> el :attrs :rx)
      (attribute.hierarchy/update-attr :rx * x)

      (-> el :attrs :ry)
      (attribute.hierarchy/update-attr :ry * y))))

(defn clamp-radius-to-size
  [el]
  (let [{:keys [attrs]} el
        width (utils.length/unit->px (:width attrs))
        height (utils.length/unit->px (:height attrs))]
    (-> el
        (attribute.hierarchy/update-attr :rx min (/ width 2))
        (attribute.hierarchy/update-attr :ry min (/ height 2)))))

(defmethod element.hierarchy/edit :rect
  [el offset handle lock?]
  (let [[x y] (cond-> offset
                (and (contains? #{:position :size} handle)
                     lock?)
                (event.handlers/lock-direction))
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

(defmethod element.hierarchy/render-edit :rect
  [el]
  (let [el-bbox (:bbox el)
        [_min-x min-y max-x _max-y] el-bbox
        {{:keys [rx ry]} :attrs} el
        [rx ry] (mapv utils.length/unit->px [rx ry])]
    [:g
     (element.impl.box/render-edit-handles el-bbox (:id el))
     (for [handle [{:x (- max-x rx)
                    :y min-y
                    :id :rx
                    :label [::rx-handle "x radius handle"]}
                   {:x max-x
                    :y (+ min-y ry)
                    :id :ry
                    :label [::ry-handle "y radius handle"]}]]
       (let [handle (merge handle {:type :handle
                                   :action :edit
                                   :element-id (:id el)})]
         ^{:key (:id handle)}
         [tool.views/circle-handle handle]))]))

(defmethod element.hierarchy/path :rect
  [el]
  (let [{{:keys [x y width height rx ry]} :attrs} el
        [x y width height] (mapv utils.length/unit->px [x y width height])
        rx (utils.length/unit->px (if (and (not rx) ry) ry rx))
        ry (utils.length/unit->px (if (and (not ry) rx) rx ry))
        rx (if (> rx (/ width 2)) (/ width 2) rx)
        ry (if (> ry (/ height 2)) (/ height 2) ry)
        curved? (and (> rx 0) (> ry 0))]
    (cond-> []
      :always (conj "M" (+ x rx) y
                    "H" (- (+ x width) rx))
      curved? (conj "A" rx ry 0 0 1 (+ x width) (+ y ry))
      :always (conj "V" (- (+ y height) ry))
      curved? (conj "A" rx ry 0 0 1 (- (+ x width) rx) (+ y height))
      :always (conj "H" (+ x rx))
      curved? (conj "A" rx ry 0 0 1 x (- (+ y height) ry))
      :always (conj "V" (+ y ry))
      curved? (conj "A" rx ry 0 0 1 (+ x rx) y)
      :always (conj "z")
      :always (->> (string/join " ")))))
