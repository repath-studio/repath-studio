(ns renderer.tool.impl.base.transform.scale
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [BBox Vec2]]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.views :as views]))

(defmethod tool.hierarchy/help [:transform :scale]
  []
  (i18n.views/t [::scale
                 [:div "Hold %1 to lock proportions, %2 to scale in place,
                        %3 to also scale children."]]
                [[views/kbd "Ctrl"]
                 [views/kbd "⇧"]
                 [views/kbd "Alt"]]))

(def ScaleHandle [:enum
                  :middle-right
                  :middle-left
                  :top-middle
                  :bottom-middle
                  :top-right
                  :top-left
                  :bottom-right
                  :bottom-left])

(def ScaleOptions
  [:map
   [:ratio-locked boolean?]
   [:in-place boolean?]
   [:recursive boolean?]])

(m/=> lock-ratio [:-> Vec2 ScaleHandle Vec2])
(defn lock-ratio
  [[x y] handle]
  (let [[x y] (condp contains? handle
                #{:middle-right :middle-left} [x x]
                #{:top-middle :bottom-middle} [y y]
                [x y])
        ratio (if (< (abs x) (abs y)) x y)]
    [ratio ratio]))

(m/=> delta->offset-with-pivot [:-> ScaleHandle Vec2 BBox [:tuple Vec2 Vec2]])
(defn delta->offset-with-pivot
  "Converts the x/y pointer offset to a scale ratio and a pivot point, to
   decouple this tool from the scaling method of the elements.

   :pivot-point
   + ─────────□──┬-------□
   │             |       |
   │             | ─ x ─ |
   │             │       │
   □ ─────────── ■       □
   |      |        ↖     │
   |      y          ↖   │
   |      |            ↖ │
   □----------□--------- ■ :bottom-right (active handle)"
  [handle offset bbox]
  (let [[x y] offset
        [min-x min-y max-x max-y] bbox
        [cx cy] (utils.bounds/center bbox)]
    (case handle
      :middle-right [[x 0] [min-x cy]]
      :middle-left [[(- x) 0] [max-x cy]]
      :top-middle [[0 (- y)] [cx max-y]]
      :bottom-middle [[0 y] [cx min-y]]
      :top-right [[x (- y)] [min-x max-y]]
      :top-left [[(- x) (- y)] [max-x max-y]]
      :bottom-right [[x y] [min-x min-y]]
      :bottom-left [[(- x) y] [max-x min-y]])))

(defn pivot-offset
  [offset handle bbox pivot-point]
  (let [dimensions (utils.bounds/->dimensions bbox)
        [px py] pivot-point
        [min-x min-y max-x max-y] bbox
        [w h] dimensions
        x-factor (condp contains? handle
                   #{:middle-right :top-right :bottom-right}
                   (/ w (- max-x px))

                   #{:middle-left :top-left :bottom-left}
                   (/ w (- px min-x))

                   1)
        y-factor (condp contains? handle
                   #{:top-middle :top-right :top-left}
                   (/ h (- py min-y))

                   #{:bottom-middle :bottom-right :bottom-left}
                   (/ h (- max-y py))

                   1)]
    (matrix/mul offset [x-factor y-factor])))

(m/=> scale [:-> App Vec2 ScaleOptions App])
(defn scale
  [db offset options]
  (let [{:keys [ratio-locked in-place recursive]} options
        handle (-> db :clicked-element :id)
        bbox (element.handlers/bbox db)
        [offset pivot-point] (delta->offset-with-pivot handle offset bbox)
        dimensions (utils.bounds/->dimensions bbox)
        pivot-point (if in-place
                      (let [[min-x min-y] bbox
                            [w h] dimensions
                            [fx fy] (:anchor-offset db)]
                        [(+ min-x (* fx w)) (+ min-y (* fy h))])
                      pivot-point)
        offset (cond-> offset
                 in-place
                 (pivot-offset handle bbox pivot-point))
        ratio (matrix/div (matrix/add dimensions offset) dimensions)
        ratio (cond-> ratio ratio-locked (lock-ratio handle))
        ;; TODO: Handle negative ratio, and position on recursive scale.
        ratio (mapv #(max % 0.01) ratio)]
    (-> db
        (assoc :pivot-point pivot-point)
        (element.handlers/scale ratio pivot-point recursive))))

(m/=> ratio-locked? [:-> App any? boolean?])
(defn ratio-locked?
  [db e]
  (or (:ctrl-key e)
      (tool.handlers/multi-touch? db)
      (element.handlers/ratio-locked? db)))

(defmethod tool.hierarchy/on-drag [:transform :scale]
  [db e]
  (let [{:keys [shift-key alt-key]} e
        delta (tool.handlers/pointer-delta db)
        selected-elements (element.handlers/selected db)
        locked? (every? :locked selected-elements)]
    (-> db
        (history.handlers/reset-state)
        (tool.handlers/set-cursor (if locked? "not-allowed" "default"))
        (scale (matrix/add delta (snap.handlers/nearest-delta db))
               {:ratio-locked (ratio-locked? db e)
                :in-place shift-key
                :recursive alt-key}))))

(defmethod tool.hierarchy/on-drag-end [:transform :scale]
  [db e]
  (-> db
      (tool.handlers/set-state :idle)
      (dissoc :clicked-element :pivot-point)
      (history.handlers/finalize (:timestamp e)
                                 [::scale-selection "Scale selection"])))

(defmethod tool.hierarchy/snapping-points [:transform :scale]
  [db]
  (when-let [el (:clicked-element db)]
    [(with-meta
       (matrix/add [(:x el) (:y el)]
                   (tool.handlers/pointer-delta db))
       {:label [::scale-handle "scale handle"]})]))

(defmethod tool.hierarchy/snapping-elements [:transform :scale]
  [db]
  (element.handlers/non-selected-visible db))
