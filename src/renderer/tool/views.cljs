(ns renderer.tool.views
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.db :refer [BBox]]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.subs :as-alias element.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.input.impl.pointer :as input.impl.pointer]
   [renderer.tool.db :refer [Handle]]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.bounds :as utils.bounds]))

(m/=> handle [:-> Handle any?])
(defn handle
  [el]
  (let [{:keys [position id cursor label rounded implied parent]} el
        clicked-element @(rf/subscribe [::app.subs/clicked-element])
        handle-size @(rf/subscribe [::document.subs/handle-size])
        zoom @(rf/subscribe [::document.subs/zoom])
        selected @(rf/subscribe [::element.subs/handle-selected? parent id])
        selected (or selected (and (= (:id clicked-element) (:id el))
                                   (= (:parent clicked-element) (:parent el))))
        hovered @(rf/subscribe [::element.subs/hovered? id])
        pointer-handler (partial input.impl.pointer/handler! el)
        active (or selected hovered)
        [x y] position
        half-size (/ handle-size 2)]
    [:g
     [:rect {:x (- x half-size)
             :y (- y half-size)
             :rx (when rounded half-size)
             :width handle-size
             :height handle-size
             :stroke-opacity ".5"
             :stroke-width (/ (if active 2 1) zoom)
             :cursor (or cursor "move")
             :pointer-events (when implied "none")
             :on-pointer-up pointer-handler
             :on-pointer-down pointer-handler
             :on-pointer-move pointer-handler
             :fill (cond
                     selected "var(--accent)"
                     implied "lightgray"
                     :else "var(--accent-foreground)")
             :stroke (cond
                       active "var(--accent)"
                       (not implied) "var(--foreground-muted)")}
      (when label [:title (i18n.views/t label)])]]))

(m/=> selected-bbox [:-> BBox any?])
(defn selected-bbox
  [bbox]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        [min-x min-y] bbox
        [w h] (utils.bounds/->dimensions bbox)
        pointer-handler (partial input.impl.pointer/handler! {:type :handle
                                                              :action :translate
                                                              :id :bbox})]
    [:rect {:x min-x
            :y min-y
            :width w
            :height h
            :stroke-opacity ".3"
            :fill "transparent"
            :shape-rendering "crispEdges"
            :stroke-width (/ 2 zoom)
            :on-pointer-up pointer-handler
            :on-pointer-down pointer-handler
            :on-pointer-move pointer-handler}]))

(m/=> min-bbox [:-> BBox BBox])
(defn min-bbox
  "Ensures the bounding box is large enough to avoid overlapping handles."
  [bbox]
  (let [dimensions (utils.bounds/->dimensions bbox)
        [w h] dimensions
        handle-size @(rf/subscribe [::document.subs/handle-size])
        min-size (* handle-size 2)]
    (cond-> bbox
      (< w min-size)
      (matrix/add [(- (/ (- min-size w) 2)) 0
                   (/ (- min-size w) 2) 0])

      (< h min-size)
      (matrix/add [0 (- (/ (- min-size h) 2))
                   0 (/ (- min-size h) 2)]))))

(m/=> corner-handles [:-> BBox any?])
(defn corner-handles
  [bbox]
  (let [idle? @(rf/subscribe [::tool.subs/idle?])
        bbox (cond-> bbox idle? min-bbox)
        [min-x min-y max-x max-y] bbox
        [w h] (utils.bounds/->dimensions bbox)]
    (->> [{:position [min-x min-y]
           :id :top-left
           :cursor "nwse-resize"}
          {:position [max-x min-y]
           :id :top-right
           :cursor "nesw-resize"}
          {:position [min-x max-y]
           :id :bottom-left
           :cursor "nesw-resize"}
          {:position [max-x max-y]
           :id :bottom-right
           :cursor "nwse-resize"}
          {:position [(+ min-x (/ w 2)) min-y]
           :id :top-middle
           :cursor "ns-resize"}
          {:position [max-x (+ min-y (/ h 2))]
           :id :middle-right
           :cursor "ew-resize"}
          {:position [min-x (+ min-y (/ h 2))]
           :id :middle-left
           :cursor "ew-resize"}
          {:position [(+ min-x (/ w 2)) max-y]
           :id :bottom-middle
           :cursor "ns-resize"}]
         (mapv (comp handle
                     (partial merge {:type :handle
                                     :action :scale})))
         (into [:g]))))
