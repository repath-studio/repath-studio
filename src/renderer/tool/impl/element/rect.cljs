(ns renderer.tool.impl.element.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement"
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.input.handlers :as input.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.key :as utils.key]
   [renderer.utils.math :as utils.math]))

(hierarchy/derive! ::rect ::tool.hierarchy/element)

(defn create-el
  [db]
  (let [parent-id (:id (element.handlers/hovered-svg db))
        to-local (partial element.handlers/container-local-point db parent-id)
        attrs (-> (document.handlers/attrs db)
                  (select-keys [:stroke :fill :stroke-width]))
        offset (tool.handlers/snapped-offset db)
        [offset-x offset-y] (to-local offset)
        [x y] (to-local (tool.handlers/snapped-position db))
        [width height] (mapv (comp utils.attribute/->fixed abs)
                             [(- x offset-x) (- y offset-y)])
        origin [(cond-> offset-x (< x offset-x) (- width))
                (cond-> offset-y (< y offset-y) (- height))]
        [x y] (mapv utils.attribute/->fixed origin)]
    (-> db
        (assoc :last-origin offset)
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :rect
                               :parent parent-id
                               :attrs (merge attrs {:x x
                                                    :y y
                                                    :width width
                                                    :height height})}))))

(defn update-el
  [db e]
  (let [{:keys [id]} (first (element.handlers/selected db))
        position (->> (tool.handlers/snapped-position db)
                      (element.handlers/local-point db id))
        origin (element.handlers/local-point db id (:last-origin db))
        position (cond->> position
                   (input.handlers/snap-to-angle? db e)
                   (input.handlers/snap-angle origin))
        size (utils.math/v-sub position origin)
        [w h] (mapv (comp utils.attribute/->fixed abs) size)
        new-x (cond-> (first origin)
                (neg? (first size))
                (+ (first size)))
        new-y (cond-> (second origin)
                (neg? (second size))
                (+ (second size)))
        [new-x new-y] (mapv utils.attribute/->fixed [new-x new-y])]
    (element.handlers/update-selected db #(-> %
                                              (assoc-in [:attrs :x] new-x)
                                              (assoc-in [:attrs :y] new-y)
                                              (assoc-in [:attrs :width] w)
                                              (assoc-in [:attrs :height] h)))))

(defn finalize
  [db e]
  (-> db
      (history.handlers/finalize (:timestamp e)
                                 [::create-rectangle "Create rectangle"])
      (tool.handlers/deactivate)))

(defmethod tool.hierarchy/on-drag-start [::rect :idle]
  [db _e]
  (create-el db))

(defmethod tool.hierarchy/on-pointer-up [::rect :idle]
  [db _e]
  (create-el db))

(defmethod tool.hierarchy/on-drag [::rect :create]
  [db e]
  (update-el db e))

(defmethod tool.hierarchy/on-pointer-down [::rect :create]
  [db e]
  (update-el db e))

(defmethod tool.hierarchy/on-pointer-move [::rect :create]
  [db e]
  (update-el db e))

(defmethod tool.hierarchy/on-drag-end [::rect :create]
  [db e]
  (finalize db e))

(defmethod tool.hierarchy/on-pointer-up [::rect :create]
  [db e]
  (finalize db e))

(rf/dispatch [::action.events/register-action
              {:id :tool/rect
               :label [::label "Rectangle"]
               :icon "rectangle-tool"
               :event [::tool.events/activate ::rect]
               :active [::tool.subs/active? ::rect]
               :shortcuts {"All" [{:keyCode (utils.key/codes "R")}]}}])
