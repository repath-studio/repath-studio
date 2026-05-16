(ns renderer.element.impl.box
  "This serves as an abstraction for box elements that share the
   :x :y :width :height attributes (e.g. rect, svg, image)."
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.hierarchy :as hierarchy]
   [renderer.input.handlers :as input.handlers]
   [renderer.tool.views :as tool.views]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]))

(hierarchy/derive! ::element.hierarchy/box ::element.hierarchy/renderable)

(defmethod element.hierarchy/translate ::element.hierarchy/box
  [el [x y]]
  (-> el
      (attribute.hierarchy/update-attr :x + x)
      (attribute.hierarchy/update-attr :y + y)))

(defmethod element.hierarchy/scale ::element.hierarchy/box
  [el ratio pivot-point]
  (let [[rx ry] ratio
        {{:keys [width height]} :attrs} el
        w (utils.length/unit->px width)
        h (utils.length/unit->px height)
        [offset-x offset-y] (utils.element/scale-offset ratio pivot-point)
        offset [(+ offset-x (min 0 (* w rx)))
                (+ offset-y (min 0 (* h ry)))]]
    (-> el
        (attribute.hierarchy/update-attr :width #(abs (* % rx)))
        (attribute.hierarchy/update-attr :height #(abs (* % ry)))
        (element.hierarchy/translate offset))))

(defmethod element.hierarchy/edit-drag ::element.hierarchy/box
  [el offset handle lock?]
  (let [[x y] (cond-> offset
                lock?
                (input.handlers/lock-direction))
        clamp (partial max 0)]
    (case handle
      :position
      (-> el
          (attribute.hierarchy/update-attr :width (comp clamp -) x)
          (attribute.hierarchy/update-attr :height (comp clamp -) y)
          (element.hierarchy/translate [x y]))

      :size
      (-> el
          (attribute.hierarchy/update-attr :width (comp clamp +) x)
          (attribute.hierarchy/update-attr :height (comp clamp +) y))

      el)))

(defn render-edit-handles
  [[min-x min-y max-x max-y] element-id]
  (->> [{:x min-x
         :y min-y
         :id :position
         :label [::position-handle "position handle"]}
        {:x max-x
         :y max-y
         :id :size
         :label [::size-handle "size handle"]}]
       (mapv (comp tool.views/handle
                   (partial merge {:type :handle
                                   :action :edit
                                   :element-id element-id})))
       (into [:g])))

(defmethod element.hierarchy/render-edit ::element.hierarchy/box
  [el]
  (render-edit-handles (:bbox el) (:id el)))

(defmethod element.hierarchy/bbox ::element.hierarchy/box
  [el]
  (let [{{:keys [x y width height]} :attrs} el
        [x y width height] (mapv utils.length/unit->px [x y width height])]
    [x y (+ x width) (+ y height)]))

(defmethod element.hierarchy/area ::element.hierarchy/box
  [el]
  (let [{{:keys [width height]} :attrs} el]
    (apply * (map utils.length/unit->px [width height]))))

#_(defmethod hierarchy/snapping-points ::element.hierarchy/box
    [el]
    (let [{{:keys [x y width height]} :attrs} el
          [x y w h] (mapv utils.length/unit->px [x y width height])]
      (mapv #(with-meta % {:label [::box-corner "box corner"]})
            [[x y]
             [(+ x w) y]
             [(+ x w) (+ y h)]
             [x (+ y h)]])))
