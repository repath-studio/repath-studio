(ns renderer.event.handlers
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.app.effects :as-alias app.effects]
   [renderer.app.handlers :as app.handlers]
   [renderer.db :refer [Vec2]]
   [renderer.effects :as-alias effects]
   [renderer.event.db :refer [PointerEvent KeyboardEvent WheelEvent DragEvent]]
   [renderer.event.effects :as-alias event.effects]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(m/=> adjusted-pointer-pos [:-> App Vec2 Vec2])
(defn adjusted-pointer-pos
  [db pos]
  (let [{:keys [zoom pan]} (get-in db [:documents (:active-document db)])]
    (-> pos
        (matrix/div zoom)
        (matrix/add pan))))

(m/=> lock-direction [:-> Vec2 Vec2])
(defn lock-direction
  "Locks pointer movement to the axis with the biggest offset"
  [[x y]]
  (if (> (abs x) (abs y))
    [x 0]
    [0 y]))

(m/=> on-pinch [:-> App PointerEvent App])
(defn on-pinch
  [db e]
  (let [{:keys [active-pointers pinch-distance pinch-midpoint]} db
        {:keys [pointer-id]} e
        active-pointers (assoc active-pointers pointer-id e)
        [pos1 pos2] (->> (vals active-pointers)
                         (take 2)
                         (map :pointer-pos))
        distance (matrix/distance pos1 pos2)
        midpoint (-> (matrix/add pos1 pos2)
                     (matrix/div 2))
        adjusted-midpoint (adjusted-pointer-pos db midpoint)]
    (cond-> db
      :always
      (assoc :active-pointers active-pointers
             :pinch-distance distance
             :pinch-midpoint midpoint)

      pinch-distance
      (-> (frame.handlers/pan-by (matrix/sub pinch-midpoint midpoint))
          (frame.handlers/zoom-at-position (/ distance pinch-distance)
                                           adjusted-midpoint)))))

(m/=> on-drag-start [:-> App PointerEvent App])
(defn on-drag-start
  [db {:keys [pointer-id]
       :as e}]
  (-> db
      (assoc :drag-pointer pointer-id)
      (tool.hierarchy/on-drag-start e)
      (app.handlers/add-fx [::event.effects/set-pointer-capture pointer-id])))

(m/=> on-drag-end [:-> App PointerEvent App])
(defn on-drag-end
  [db {:keys [pointer-id]
       :as e}]
  (-> db
      (tool.hierarchy/on-drag-end e)
      (tool.handlers/clear-pointer-data)
      (app.handlers/add-fx [::event.effects/release-pointer-capture
                            pointer-id])))

(m/=> drag-pointer? [:-> App PointerEvent boolean?])
(defn drag-pointer?
  [db e]
  (= (:drag-pointer db)
     (:pointer-id e)))

(m/=> on-drag [:-> App PointerEvent App])
(defn on-drag
  [db e]
  (let [{:keys [pointer-offset tool dom-rect drag-pointer]} db
        {:keys [pointer-pos]} e]
    (cond-> db
      (and (not= tool :pan)
           (drag-pointer? db e))
      (tool.handlers/pan-out-of-canvas dom-rect pointer-pos pointer-offset)

      (not drag-pointer)
      (on-drag-start e)

      :always
      (tool.hierarchy/on-drag e))))

(m/=> significant-drag? [:-> App PointerEvent boolean?])
(defn significant-drag?
  [db e]
  (let [{:keys [pointer-offset drag-threshold]} db
        {:keys [pointer-pos]} e]
    (> (matrix/distance pointer-offset pointer-pos)
       drag-threshold)))

(m/=> on-pointer-move [:-> App PointerEvent App])
(defn on-pointer-move
  [db e]
  (let [{:keys [pointer-offset drag-pointer active-pointers]} db
        {:keys [pointer-pos pointer-id]} e]
    (if (and (tool.handlers/multi-touch? db) (not drag-pointer))
      (on-pinch db e)
      (cond-> (if (and pointer-offset
                       (contains? active-pointers pointer-id)
                       (significant-drag? db e))
                (on-drag db e)
                (tool.hierarchy/on-pointer-move db e))

        (or (drag-pointer? db e) (not drag-pointer))
        (assoc :pointer-pos pointer-pos
               :adjusted-pointer-pos (adjusted-pointer-pos db pointer-pos))))))

(m/=> on-pointer-down [:-> App PointerEvent App])
(defn on-pointer-down
  [db e]
  (let [{:keys [tool state nearest-neighbor active-pointers]} db
        {:keys [button pointer-pos pointer-id]} e]
    (cond-> db
      (not= button :right)
      (assoc-in [:active-pointers pointer-id] e)

      (= button :middle)
      (-> (assoc :cached-tool tool
                 :cached-state state)
          (tool.handlers/activate :pan))

      (and (not= button :right)
           (empty? active-pointers))
      (assoc :pointer-offset pointer-pos
             :adjusted-pointer-offset (adjusted-pointer-pos db pointer-pos)
             :nearest-neighbor-offset (:point nearest-neighbor))

      (empty? active-pointers)
      (-> (tool.hierarchy/on-pointer-down e)
          (app.handlers/add-fx [::effects/focus-canvas nil])))))

(m/=> db-click? [:-> App PointerEvent boolean?])
(defn db-click?
  [db e]
  (let [{:keys [double-click-delta event-timestamp]} db
        {:keys [timestamp]} e]
    (< 0 (- timestamp event-timestamp) double-click-delta)))

(m/=> on-pointer-up [:-> App PointerEvent App])
(defn on-pointer-up
  [db e]
  (let [{:keys [cached-tool cached-state active-pointers pinch-distance
                drag-pointer]} db
        {:keys [button timestamp pointer-id]} e
        db (snap.handlers/update-nearest-neighbors db)]
    (if pinch-distance
      (cond-> db
        :always
        (update :active-pointers dissoc pointer-id)

        (<= (count active-pointers) 2)
        (-> (tool.handlers/clear-pointer-data)
            (snap.handlers/update-viewport-tree)))
      (cond-> (if drag-pointer
                (cond-> db
                  (drag-pointer? db e)
                  (on-drag-end e))
                (if (db-click? db e)
                  (-> (dissoc db :event-timestamp)
                      (tool.hierarchy/on-double-click e))
                  (-> (assoc db :event-timestamp timestamp)
                      (tool.hierarchy/on-pointer-up e))))
        (and cached-tool (= button :middle))
        (-> (tool.handlers/activate cached-tool)
            (tool.handlers/set-state cached-state)
            (dissoc :cached-tool :cached-state))

        (not (tool.handlers/multi-touch? db))
        (tool.handlers/clear-pointer-data)

        :always
        (update :active-pointers dissoc pointer-id)))))

(m/=> pointer [:-> App PointerEvent App])
(defn pointer
  [db e]
  (let [{:keys [active-pointers]} db
        {:keys [pointer-id button]} e
        db (snap.handlers/update-nearest-neighbors db)]
    (case (:type e)
      "pointermove"
      (on-pointer-move db e)

      "pointerdown"
      (on-pointer-down db e)

      "pointerup"
      (cond-> db
        (and (contains? active-pointers pointer-id)
             (not= button :right))
        (on-pointer-up e))

      db)))

(m/=> on-key-down [:-> App KeyboardEvent App])
(defn on-key-down
  [db e]
  (cond-> db
    (and (= (:code e) "Space")
         (not= (:tool db) :pan)
         (= (:state db) :idle))
    (-> (assoc :cached-tool (:tool db))
        (tool.handlers/activate :pan))

    (= (:key e) "Shift")
    (-> (assoc-in [:snap :transient-active] true)
        (cond->
         (not (-> db :snap :active))
          (-> (dissoc :nearest-neighbor)
              (snap.handlers/rebuild-tree))))

    (and (= (:key e) "Alt")
         (= (:state db) :idle))
    (assoc-in [:menubar :indicator] true)

    :always
    (tool.hierarchy/on-key-down e)))

(m/=> on-key-up [:-> App KeyboardEvent App])
(defn on-key-up
  [db e]
  (cond-> db
    (and (= (:code e) "Space")
         (:cached-tool db))
    (-> (tool.handlers/activate (:cached-tool db))
        (dissoc :cached-tool))

    (= (:key e) "Shift")
    (-> (assoc-in [:snap :transient-active] false)
        (cond->
         (not (-> db :snap :active))
          (dissoc :nearest-neighbor)))

    (= (:key e) "Alt")
    (assoc-in [:menubar :indicator] false)

    :always
    (tool.hierarchy/on-key-up e)))

(m/=> keyboard [:-> App KeyboardEvent App])
(defn keyboard
  [db e]
  (case (:type e)
    "keydown"
    (on-key-down db e)

    "keyup"
    (on-key-up db e)

    db))

(m/=> wheel [:-> App WheelEvent App])
(defn wheel
  [db e]
  (let [{:keys [delta-x delta-y ctrl-key shift-key]} e]
    (-> (if (or ctrl-key shift-key)
          (let [factor (-> (:zoom-sensitivity db)
                           dec (/ 100) inc
                           (Math/pow delta-y))]
            (frame.handlers/zoom-at-pointer db factor))
          (frame.handlers/pan-by db [delta-x delta-y]))
        (snap.handlers/update-viewport-tree)
        (app.handlers/add-fx [::app.effects/persist]))))

(m/=> drag [:-> App DragEvent App])
(defn drag
  [db e]
  (case (:type e)
    "drop"
    (let [{:keys [data-transfer pointer-pos]} e
          position (adjusted-pointer-pos db pointer-pos)]
      (app.handlers/add-fx db [::event.effects/drop [position data-transfer]]))

    db))
