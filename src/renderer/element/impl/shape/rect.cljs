(ns renderer.element.impl.shape.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/rect"
  (:require
   [clojure.string :as string]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.event.handlers :as event.handlers]
   [renderer.tool.views :as tool.views]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.length :as utils.length]))

(derive :rect ::element.hierarchy/box)
(derive :rect ::element.hierarchy/shape)

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

(defmethod element.hierarchy/edit :rect
  [el offset handle e]
  (let [[x y] (cond-> offset
                (and (contains? #{:position :size} handle)
                     (:ctrl-key e))
                (event.handlers/lock-direction))
        clamp-size (partial max 0)
        el-bbox (:bbox el)
        [w h] (utils.bounds/->dimensions el-bbox)
        locked-radius? (:ctrl-key e)
        clamp-radius (fn [r max-size]
                       (min (max 0 r)
                            (/ (if locked-radius?
                                 (min w h)
                                 max-size) 2)))]
    (case handle
      :position
      (-> el
          (attribute.hierarchy/update-attr :width (comp clamp-size -) x)
          (attribute.hierarchy/update-attr :height (comp clamp-size -) y)
          (element.hierarchy/translate [x y]))

      :size
      (-> el
          (attribute.hierarchy/update-attr :width (comp clamp-size +) x)
          (attribute.hierarchy/update-attr :height (comp clamp-size +) y))

      :rx
      (cond-> el
        :always
        (attribute.hierarchy/update-attr :rx (comp #(clamp-radius % w) -) x)

        locked-radius?
        (update :attrs (fn [attrs] (assoc attrs :ry (:rx attrs)))))

      :ry
      (cond-> el
        :always
        (attribute.hierarchy/update-attr :ry (comp #(clamp-radius % h) +) y)

        locked-radius?
        (update :attrs (fn [attrs] (assoc attrs :rx (:ry attrs)))))

      el)))

(defmethod element.hierarchy/render-edit :rect
  [el]
  (let [el-bbox (:bbox el)
        [min-x min-y max-x max-y] el-bbox
        {{:keys [rx ry]} :attrs} el
        [rx ry] (mapv utils.length/unit->px [rx ry])]
    [:g
     (for [handle [{:x min-x
                    :y min-y
                    :id :position
                    :label [::position-handle "position handle"]}
                   {:x max-x
                    :y max-y
                    :id :size
                    :label [::size-handle "size handle"]}]]
       (let [handle (merge handle {:type :handle
                                   :action :edit
                                   :element-id (:id el)})]
         ^{:key (:id handle)}
         [tool.views/square-handle handle]))
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
