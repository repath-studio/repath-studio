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

(m/=> significant-drag? [:-> App Vec2 Vec2 boolean?])
(defn significant-drag?
  [db position offset]
  (> (apply max (map abs (matrix/sub position offset)))
     (:drag-threshold db)))

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

(m/=> pointer [:-> App PointerEvent App])
(defn pointer
  [db e]
  (let [{:keys [pointer-offset tool state cached-tool cached-state dom-rect
                nearest-neighbor active-pointers double-click-delta
                event-timestamp pinch-distance pinch-midpoint drag-pointer]} db
        {:keys [button pointer-pos timestamp pointer-id]} e
        adjusted-pos (adjusted-pointer-pos db pointer-pos)
        db (snap.handlers/update-nearest-neighbors db)]
    (case (:type e)
      "pointermove"
      (if (contains? active-pointers pointer-id)
        (if (and (not drag-pointer)
                 (> (count active-pointers) 1))
          (let [active-pointers (assoc active-pointers pointer-id e)
                [pos1 pos2] (map :pointer-pos (take 2 (vals active-pointers)))
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
                                                   adjusted-midpoint))))
          (cond-> (if pointer-offset
                    (if (significant-drag? db pointer-pos pointer-offset)
                      (cond-> db
                        (not= tool :pan)
                        (tool.handlers/pan-out-of-canvas dom-rect
                                                         pointer-pos
                                                         pointer-offset)

                        (not drag-pointer)
                        (-> (assoc :drag-pointer pointer-id)
                            (tool.hierarchy/on-drag-start e)
                            (app.handlers/add-fx
                             [::event.effects/set-pointer-capture pointer-id]))

                        :always
                        (tool.hierarchy/on-drag e))
                      db)
                    (tool.hierarchy/on-pointer-move db e))

            (or (= drag-pointer pointer-id) (not drag-pointer))
            (assoc :pointer-pos pointer-pos
                   :adjusted-pointer-pos adjusted-pos)))
        db)

      "pointerdown"
      (cond-> db
        (not= button :right)
        (assoc-in [:active-pointers pointer-id] e)

        (= button :middle)
        (-> (assoc :cached-tool tool
                   :cached-state state)
            (tool.handlers/activate :pan))

        (and (not= button :right)
             (not (seq active-pointers)))
        (assoc :pointer-offset pointer-pos
               :adjusted-pointer-offset adjusted-pos
               :nearest-neighbor-offset (:point nearest-neighbor))

        (not (seq active-pointers))
        (-> (tool.hierarchy/on-pointer-down e)
            (app.handlers/add-fx [::effects/focus-canvas nil])))

      "pointerup"
      (if (contains? active-pointers pointer-id)
        (if pinch-distance
          (cond-> db
            :always
            (update :active-pointers dissoc pointer-id)

            (<= (count active-pointers) 2)
            (-> (assoc :active-pointers {})
                (dissoc :pinch-distance :pinch-midpoint
                        :pointer-offset :drag-pointer :nearest-neighbor)
                (snap.handlers/update-viewport-tree)))
          (cond-> (if drag-pointer
                    (cond-> db
                      (= drag-pointer pointer-id)
                      (-> (tool.hierarchy/on-drag-end e)
                          (assoc :active-pointers {})
                          (dissoc :pointer-offset :drag-pointer :nearest-neighbor)
                          (app.handlers/add-fx
                           [::event.effects/release-pointer-capture pointer-id])))
                    (if (= button :right)
                      db
                      (if (< 0 (- timestamp event-timestamp) double-click-delta)
                        (-> (dissoc db :event-timestamp)
                            (tool.hierarchy/on-double-click e))
                        (-> (assoc db :event-timestamp timestamp)
                            (tool.hierarchy/on-pointer-up e)))))
            (and cached-tool (= button :middle))
            (-> (tool.handlers/activate cached-tool)
                (tool.handlers/set-state cached-state)
                (dissoc :cached-tool :cached-state))

            :always
            (update :active-pointers dissoc pointer-id)))
        db)
      db)))

(m/=> keyboard [:-> App KeyboardEvent App])
(defn keyboard
  [db e]
  (case (:type e)
    "keydown"
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
      (tool.hierarchy/on-key-down e))

    "keyup"
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
      (tool.hierarchy/on-key-up e))
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
