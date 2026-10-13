(ns renderer.tool.impl.base.transform.rotate
  (:require
   [clojure.math :as math]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [BBox Vec2]]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.subs :as-alias element.subs]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.input.handlers :as input.handlers]
   [renderer.input.subs :as-alias input.subs]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.transform.core :as-alias transform]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.tool.views :as tool.views]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.svg :as utils.svg]
   [renderer.views :as views]))

(defmethod tool.hierarchy/help [::transform/transform :rotate]
  []
  (i18n.views/t [::rotate
                 [:div "Drag to rotate the selection about the pivot point.\n
                         Hold %1 to snap to 15° increments."]]
                [[views/kbd "Ctrl"]]))

(m/=> pivot [:-> App [:maybe Vec2]])
(defn pivot
  [db]
  (let [bbox (element.handlers/bbox db)]
    (when bbox
      (let [[min-x min-y] bbox
            [w h] (utils.bounds/->dimensions bbox)
            [fx fy] (:anchor-offset db)]
        [(+ min-x (* fx w)) (+ min-y (* fy h))]))))

(m/=> ->anchor-offset [:-> BBox Vec2 Vec2])
(defn ->anchor-offset
  "The [0 1] anchor offset that resolves to the given point within the bounds.
   Inverse of `pivot`; used to keep the pivot in place after the bounds change."
  [bbox [px py]]
  (let [[min-x min-y] bbox
        [w h] (utils.bounds/->dimensions bbox)]
    [(/ (- px min-x) w)
     (/ (- py min-y) h)]))

(m/=> drag-angle [:-> App Vec2 number?])
(defn drag-angle
  [db p]
  (let [[sx sy] (:local-pointer-offset db)
        [cx cy] (:local-pointer-pos db)
        [px py] p
        start (Math/atan2 (- sy py) (- sx px))
        current (Math/atan2 (- cy py) (- cx px))]
    (math/to-degrees (- current start))))

(defmethod tool.hierarchy/on-drag [::transform/transform :rotate]
  [db e]
  (let [db (history.handlers/reset-state db)
        p (pivot db)
        angle (drag-angle db p)
        angle (if (input.handlers/snap-to-angle? db e)
                (input.handlers/snap-angle angle) angle)]
    (cond-> db
      (number? angle)
      (-> (assoc :pivot-point p)
          (element.handlers/rotate angle p)
          (tool.handlers/set-cursor "crosshair")))))

(defmethod tool.hierarchy/on-drag-end [::transform/transform :rotate]
  [db e]
  (let [anchor (some-> (element.handlers/bbox db)
                       (->anchor-offset (:pivot-point db)))]
    (cond-> db
      :always
      (tool.handlers/set-state :idle)

      anchor
      (assoc :anchor-offset anchor
             :anchor-point anchor)

      :always
      (-> (dissoc :clicked-element :pivot-point)
          (history.handlers/finalize (:timestamp e) [::rotate-selection
                                                     "Rotate selection"])))))

(defn handle
  []
  (let [state @(rf/subscribe [::tool.subs/state])
        bbox @(rf/subscribe [::element.subs/bbox])
        anchor-offset @(rf/subscribe [::tool.subs/anchor-offset])
        pivot-point @(rf/subscribe [::tool.subs/pivot-point])
        zoom @(rf/subscribe [::document.subs/zoom])
        pointer-pos @(rf/subscribe [::input.subs/local-pointer-pos])]
    (when bbox
      (let [[min-x min-y] bbox
            [w h] (utils.bounds/->dimensions bbox)
            [fx fy] anchor-offset
            p (or (when (= state :rotate) pivot-point)
                  [(+ min-x (* fx w)) (+ min-y (* fy h))])
            y-offset (+ (/ 32 zoom) (/ h 2))
            position (if (= state :rotate)
                       pointer-pos
                       [(first p) (- (second p) y-offset)])]
        [:<>
         [utils.svg/arm p (if (= state :rotate) pointer-pos position)]
         [tool.views/handle {:id :rotate-handle
                             :position position
                             :rounded true
                             :type :handle
                             :action :rotate
                             :label [::rotate-handle "rotate handle"]}]]))))
