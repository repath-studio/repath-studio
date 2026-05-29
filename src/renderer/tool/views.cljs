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
  (let [{:keys [position id cursor label orientation rounded implied parent]} el
        [x y] position
        zoom @(rf/subscribe [::document.subs/zoom])
        clicked-element @(rf/subscribe [::app.subs/clicked-element])
        handle-size @(rf/subscribe [::document.subs/handle-size])
        selected? @(rf/subscribe [::element.subs/handle-selected? parent id])
        hovered? @(rf/subscribe [::element.subs/hovered? id])
        pointer-handler (partial input.impl.pointer/handler! el)
        vertical-size (cond-> handle-size (= orientation :vertical) (* 0.7))
        horizontal-size (cond-> handle-size (= orientation :horizontal) (* 0.7))
        active (or selected? (= clicked-element el))
        attrs {:x (- x (/ horizontal-size 2))
               :y (- y (/ vertical-size 2))
               :rx (when rounded (/ handle-size 2))
               :width horizontal-size
               :height vertical-size}]
    [:g (when implied {:pointer-events "none"
                       :opacity 0.5})
     [:rect (merge attrs
                   {:stroke "var(--accent-foreground)"
                    :stroke-opacity ".5"
                    :stroke-width (/ (if (or active hovered?) 5 3) zoom)
                    :cursor (or cursor "move")
                    :on-pointer-up pointer-handler
                    :on-pointer-down pointer-handler
                    :on-pointer-move pointer-handler})
      (when label [:title (i18n.views/t label)])]
     [:rect (merge attrs
                   {:fill (if active
                            "var(--accent)"
                            "var(--accent-foreground)")
                    :stroke (if (or active hovered?)
                              "var(--accent)"
                              "var(--foreground-muted)")
                    :pointer-events "none"
                    :stroke-width (/ (if (or active hovered?) 2 1) zoom)})]]))

(m/=> selected-bbox [:-> BBox any?])
(defn selected-bbox
  [bbox]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        [min-x min-y] bbox
        [w h] (utils.bounds/->dimensions bbox)
        pointer-handler (partial input.impl.pointer/handler! {:type :handle
                                                              :action :translate
                                                              :id :bbox})
        rect-attrs {:x min-x
                    :y min-y
                    :width w
                    :height h
                    :stroke-opacity ".3"
                    :fill "transparent"
                    :shape-rendering "crispEdges"}]
    [:rect (merge rect-attrs {:stroke-width (/ 2 zoom)
                              :on-pointer-up pointer-handler
                              :on-pointer-down pointer-handler
                              :on-pointer-move pointer-handler})]))

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
