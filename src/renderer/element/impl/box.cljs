(ns renderer.element.impl.box
  "This serves as an abstraction for box elements that share the
   :x :y :width :height attributes (e.g. rect, svg, image)."
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.event.handlers :as event.handlers]
   [renderer.tool.views :as tool.views]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]))

(derive ::element.hierarchy/box ::element.hierarchy/renderable)

(defmethod element.hierarchy/translate ::element.hierarchy/box
  [el [x y]]
  (-> el
      (attribute.hierarchy/update-attr :x + x)
      (attribute.hierarchy/update-attr :y + y)))

(defmethod element.hierarchy/scale ::element.hierarchy/box
  [el ratio pivot-point]
  (let [[x y] ratio
        offset (utils.element/scale-offset ratio pivot-point)]
    (-> el
        (attribute.hierarchy/update-attr :width * x)
        (attribute.hierarchy/update-attr :height * y)
        (element.hierarchy/translate offset))))

(defmethod element.hierarchy/edit ::element.hierarchy/box
  [el offset handle e]
  (let [[x y] (cond-> offset
                (:ctrl-key e)
                (event.handlers/lock-direction))
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
                                 :element-id element-id})]
       ^{:key (:id handle)}
       [tool.views/square-handle handle]))])

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
