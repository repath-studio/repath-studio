(ns renderer.tool.views
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.db :refer [BBox]]
   [renderer.document.subs :as-alias document.subs]
   [renderer.event.impl.pointer :as event.impl.pointer]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.bounds :as utils.bounds]))

(defn circle-handle
  [el]
  (let [{:keys [x y id element-id label]} el
        zoom @(rf/subscribe [::document.subs/zoom])
        clicked-element @(rf/subscribe [::app.subs/clicked-element])
        handle-size @(rf/subscribe [::document.subs/handle-size])
        pointer-handler (partial event.impl.pointer/handler! el)
        r (/ handle-size 2)
        active (and (= (:id clicked-element) id)
                    (= (:element-id clicked-element) element-id))]
    [:g
     [:circle {:cx x
               :cy y
               :stroke "var(--accent-foreground)"
               :stroke-opacity ".5"
               :stroke-width (/ 3 zoom)
               :r r
               :cursor "default"
               :on-pointer-up pointer-handler
               :on-pointer-down pointer-handler
               :on-pointer-move pointer-handler}
      (when label
        [:title (i18n.views/t label)])]
     [:circle {:cx x
               :cy y
               :stroke (if active "var(--accent)" "var(--foreground-muted)")
               :stroke-width (/ 1 zoom)
               :fill (if active "var(--accent)" "var(--primary)")
               :r r
               :pointer-events "none"}]]))

(defn square-handle
  [el]
  (let [{:keys [x y id cursor element-id label]} el
        zoom @(rf/subscribe [::document.subs/zoom])
        clicked-element @(rf/subscribe [::app.subs/clicked-element])
        handle-size @(rf/subscribe [::document.subs/handle-size])
        pointer-handler (partial event.impl.pointer/handler! el)
        stroke-width (/ 1 zoom)
        rx (/ 1 zoom)
        x (- x (/ handle-size 2))
        y (- y (/ handle-size 2))
        active (and (= (:id clicked-element) id)
                    (= (:element-id clicked-element) element-id))]
    [:g
     [:rect {:stroke "var(--accent-foreground)"
             :stroke-opacity ".5"
             :stroke-width (/ 3 zoom)
             :x x
             :y y
             :rx rx
             :width handle-size
             :height handle-size
             :cursor (or cursor "move")
             :on-pointer-up pointer-handler
             :on-pointer-down pointer-handler
             :on-pointer-move pointer-handler}
      (when label
        [:title (i18n.views/t label)])]
     [:rect {:fill (if active "var(--accent)" "var(--accent-foreground)")
             :stroke (if active "var(--accent)" "var(--foreground-muted)")
             :stroke-width stroke-width
             :x x
             :y y
             :rx rx
             :width handle-size
             :height handle-size
             :pointer-events "none"}]]))

(m/=> wrapping-bbox [:-> BBox any?])
(defn wrapping-bbox
  [bbox]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        id :bbox
        ignored-ids @(rf/subscribe [::document.subs/ignored-ids])
        ignored? (contains? ignored-ids id)
        [min-x min-y] bbox
        [w h] (utils.bounds/->dimensions bbox)
        pointer-handler (partial event.impl.pointer/handler! {:type :handle
                                                              :action :translate
                                                              :id id})
        rect-attrs {:x min-x
                    :y min-y
                    :width w
                    :height h
                    :stroke-opacity ".3"
                    :fill "transparent"
                    :shape-rendering "crispEdges"}]
    [:g
     [:rect (merge rect-attrs {:stroke-width (/ 2 zoom)
                               :stroke "var(--accent-foreground)"
                               :pointer-events (when ignored? "none")
                               :on-pointer-up pointer-handler
                               :on-pointer-down pointer-handler
                               :on-pointer-move pointer-handler})]
     [:rect (merge rect-attrs {:stroke-width (/ 1 zoom)
                               :pointer-events "none"
                               :stroke "var(--accent)"})]]))

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

(m/=> bounding-corners [:-> BBox any?])
(defn bounding-corners
  [bbox]
  (let [state @(rf/subscribe [::tool.subs/state])
        clicked-element @(rf/subscribe [::app.subs/clicked-element])
        bbox (cond-> bbox (= state :idle) min-bbox)
        [min-x min-y max-x max-y] bbox
        [w h] (utils.bounds/->dimensions bbox)]
    [:g {:key :bounding-corners}
     (->> [{:x min-x
            :y min-y
            :id :top-left
            :cursor "nwse-resize"}
           {:x max-x
            :y min-y
            :id :top-right
            :cursor "nesw-resize"}
           {:x min-x
            :y max-y
            :id :bottom-left
            :cursor "nesw-resize"}
           {:x max-x
            :y max-y
            :id :bottom-right
            :cursor "nwse-resize"}
           {:x (+ min-x (/ w 2))
            :y min-y
            :id :top-middle
            :cursor "ns-resize"}
           {:x max-x
            :y (+ min-y (/ h 2))
            :id :middle-right
            :cursor "ew-resize"}
           {:x min-x
            :y (+ min-y (/ h 2))
            :id :middle-left
            :cursor "ew-resize"}
           {:x (+ min-x (/ w 2))
            :y max-y
            :id :bottom-middle
            :cursor "ns-resize"}]
          (map #(when (or (= state :idle)
                          (and (= state :scale)
                               (= (:id %) (:id clicked-element))))
                  ^{:key (:id %)}
                  [square-handle (merge % {:type :handle
                                           :action :scale})])))]))
