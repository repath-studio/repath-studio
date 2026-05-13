(ns renderer.input.handlers
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [Vec2]]
   [renderer.input.db :refer [DragEvent KeyboardEvent PointerEvent WheelEvent]]
   [renderer.input.hierarchy :as input.hierarchy]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.utils.math :as utils.math]))

(m/=> lock-direction [:-> Vec2 Vec2])
(defn lock-direction
  "Locks pointer movement to the axis with the biggest offset"
  [[x y]]
  (if (> (abs x) (abs y))
    [x 0]
    [0 y]))

(m/=> adjusted-pos [:-> App Vec2 Vec2])
(defn adjusted-pos
  [db pos]
  (let [{:keys [zoom pan]} (get-in db [:documents (:active-document db)])]
    (-> pos
        (matrix/div zoom)
        (matrix/add pan))))

(m/=> snap-angle [:-> Vec2 Vec2 Vec2])
(defn snap-angle
  "Snaps the end position to the nearest 15 degree angle."
  [start end]
  (let [degrees (utils.math/angle start end)
        snapped (* (Math/round (/ degrees 15)) 15)
        r (matrix/distance start end)
        [x1 y1] start]
    [(+ x1 (utils.math/angle-dx snapped r))
     (+ y1 (utils.math/angle-dy snapped r))]))

(defn multi-touch?
  [db]
  (> (count (:active-pointers db)) 1))

(m/=> clear-pointer-data [:-> App App])
(defn clear-pointer-data
  [db]
  (-> db
      (assoc :active-pointers {})
      (dissoc :drag-pointer :pointer-offset :adjusted-pointer-offset
              :nearest-neighbor :nearest-neighbor-offset
              :pinch-distance :pinch-midpoint)))

(m/=> pointer [:-> App PointerEvent App])
(defn pointer
  [db e]
  (-> db
      (snap.handlers/update-nearest-neighbors)
      (input.hierarchy/pointer e)))

(m/=> keyboard [:-> App KeyboardEvent App])
(defn keyboard
  [db e]
  (input.hierarchy/keyboard db e))

(m/=> wheel [:-> App WheelEvent App])
(defn wheel
  [db e]
  (input.hierarchy/wheel db e))

(m/=> drag [:-> App DragEvent App])
(defn drag
  [db e]
  (input.hierarchy/wheel db e))
