(ns renderer.input.impl.pointer
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.db :refer [App]]
   [renderer.app.handlers :as app.handlers]
   [renderer.db :refer [JS_Object Vec2]]
   [renderer.effects :as-alias effects]
   [renderer.element.db :refer [Element]]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.input.db :refer [PointerEvent PointerButton]]
   [renderer.input.effects :as-alias input.effects]
   [renderer.input.events :as-alias input.events]
   [renderer.input.handlers :as input.handlers]
   [renderer.input.hierarchy :as input.hierarchy]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.db :refer [Handle]]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.pan :as-alias tool.impl.base.pan]))

(m/=> button->key [:-> [:enum -1 0 1 2 3 4] [:maybe PointerButton]])
(defn button->key
  "https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent/button"
  [button]
  (get {0 :left
        1 :middle
        2 :right
        3 :back
        4 :forward} button))

(m/=> ->clj [:-> [:or Element Handle] JS_Object PointerEvent])
(defn ->clj
  "https://developer.mozilla.org/en-US/docs/Web/API/PointerEvent"
  [el ^js/PointerEvent e]
  {:element el
   :target (.-target e)
   :type (.-type e)
   :pointer-pos [(.-pageX e) (.-pageY e)]
   :pressure (.-pressure e)
   :pointer-type (.-pointerType e)
   :pointer-id (.-pointerId e)
   :timestamp (.-timeStamp e)
   :primary (.-isPrimary e)
   :button (button->key (.-button e))
   :alt-key (.-altKey e)
   :ctrl-key (.-ctrlKey e)
   :meta-key (.-metaKey e)
   :shift-key (.-shiftKey e)})

(m/=> handler! [:-> [:or Element Handle] JS_Object nil?])
(defn handler!
  "Gathers pointer event props and dispathces the corresponding event.
   https://day8.github.io/re-frame/FAQs/Null-Dispatched-Events/"
  [el ^js/PointerEvent e]
  (.stopPropagation e)
  (.preventDefault e)

  ;; Although the fps might drop because synced dispatch blocks rendering,
  ;; the end result appears to be more responsive because it's synced with the
  ;; pointer movement.
  (rf/dispatch-sync [::input.events/pointer (->clj el e)]))

(m/=> drag-pointer? [:-> App PointerEvent boolean?])
(defn drag-pointer?
  [db e]
  (= (:drag-pointer db)
     (:pointer-id e)))

(m/=> on-drag-start [:-> App PointerEvent App])
(defn on-drag-start
  [db {:keys [pointer-id]
       :as e}]
  (-> db
      (assoc :drag-pointer pointer-id)
      (tool.hierarchy/on-drag-start e)
      (app.handlers/add-fx [::input.effects/set-pointer-capture pointer-id])))

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
        adjusted-midpoint (input.handlers/adjusted-pos db midpoint)]
    (cond-> db
      :always
      (assoc :active-pointers active-pointers
             :pinch-distance distance
             :pinch-midpoint midpoint)

      pinch-distance
      (-> (frame.handlers/pan-by (matrix/sub pinch-midpoint midpoint))
          (frame.handlers/zoom-at-position (/ distance pinch-distance)
                                           adjusted-midpoint)))))

(m/=> on-drag [:-> App PointerEvent App])
(defn on-drag
  [db e]
  (let [{:keys [pointer-offset tool dom-rect drag-pointer]} db
        {:keys [pointer-pos]} e]
    (cond-> db
      (and (not= tool ::tool.impl.base.pan/pan)
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

(m/=> drag? [:-> App PointerEvent boolean?])
(defn drag?
  [db e]
  (let [{:keys [pointer-offset active-pointers]} db
        {:keys [pointer-id]} e]
    (and pointer-offset
         (contains? active-pointers pointer-id)
         (significant-drag? db e))))

(m/=> snap-to-angle? [:-> App PointerEvent boolean?])
(defn snap-to-angle?
  [db e]
  (and (:pointer-offset db)
       (or (:ctrl-key e)
           (input.handlers/multi-touch? db))))

(m/=> adjusted-pointer-pos [:-> App PointerEvent Vec2])
(defn adjusted-pointer-pos
  [db e]
  (let [{:keys [adjusted-pointer-offset]} db
        {:keys [pointer-pos]} e]
    (cond->> (input.handlers/adjusted-pos db pointer-pos)
      (snap-to-angle? db e)
      (input.handlers/snap-angle adjusted-pointer-offset))))

(defmethod input.hierarchy/pointer "pointermove"
  [db e]
  (let [{:keys [drag-pointer]} db
        {:keys [pointer-pos]} e
        multi-touch? (input.handlers/multi-touch? db)]
    (if (and multi-touch? (not drag-pointer))
      (on-pinch db e)
      (cond-> (if (drag? db e)
                (on-drag db e)
                (tool.hierarchy/on-pointer-move db e))

        (or (drag-pointer? db e) (not drag-pointer))
        (assoc :pointer-pos pointer-pos
               :adjusted-pointer-pos (adjusted-pointer-pos db e))))))

(defmethod input.hierarchy/pointer "pointerdown"
  [db e]
  (let [{:keys [nearest-neighbor active-pointers]} db
        {:keys [button pointer-pos pointer-id]} e]
    (cond-> db
      (not= button :right)
      (assoc-in [:active-pointers pointer-id] e)

      (= button :middle)
      (-> (tool.handlers/set-cached ::tool.impl.base.pan/pan))

      (or (= button :middle)
          (and (= button :left) (empty? active-pointers)))
      (assoc :pointer-offset pointer-pos
             :adjusted-pointer-offset (input.handlers/adjusted-pos db
                                                                   pointer-pos)
             :nearest-neighbor-offset (:point nearest-neighbor))

      (or (= button :middle)
          (empty? active-pointers))
      (-> (tool.hierarchy/on-pointer-down e)
          (app.handlers/add-fx [::effects/focus-canvas nil])))))

(m/=> db-click? [:-> App PointerEvent boolean?])
(defn db-click?
  [db e]
  (let [{:keys [double-click-delta event-timestamp]} db
        {:keys [timestamp]} e
        timestamp-delta (- timestamp event-timestamp)]
    (< 0 timestamp-delta double-click-delta)))

(m/=> on-drag-end [:-> App PointerEvent App])
(defn on-drag-end
  [db {:keys [pointer-id]
       :as e}]
  (-> db
      (tool.hierarchy/on-drag-end e)
      (input.handlers/clear-pointer-data)
      (app.handlers/add-fx [::input.effects/release-pointer-capture
                            pointer-id])))

(m/=> on-pointer-up [:-> App PointerEvent App])
(defn on-pointer-up
  [db e]
  (let [{:keys [cached-tool active-pointers pinch-distance drag-pointer]} db
        {:keys [button timestamp pointer-id]} e]
    (if pinch-distance
      (cond-> db
        :always
        (update :active-pointers dissoc pointer-id)

        (<= (count active-pointers) 2)
        (-> (input.handlers/clear-pointer-data)
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
        (tool.handlers/reset-cached)

        (not (input.handlers/multi-touch? db))
        (input.handlers/clear-pointer-data)

        :always
        (update :active-pointers dissoc pointer-id)))))

(defmethod input.hierarchy/pointer "pointerup"
  [db e]
  (let [{:keys [active-pointers]} db
        {:keys [button pointer-id]} e]
    (cond-> db
      (and (contains? active-pointers pointer-id)
           (not= button :right))
      (on-pointer-up e))))

(defmethod input.hierarchy/pointer "contextmenu"
  [db e]
  (tool.hierarchy/on-context-menu db e))
