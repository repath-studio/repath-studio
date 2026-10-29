(ns renderer.tool.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [Vec2 DomRect]]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.handlers]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.input.handlers :as input.handlers]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.db :refer [ToolHistory Tool State Cursor]]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.edit.core :as-alias tool.impl.base.edit]
   [renderer.tool.impl.base.transform.core :as-alias tool.impl.base.transform]
   [renderer.utils.math :as utils.math]))

(m/=> tool-history [:-> App [:maybe ToolHistory]])
(defn tool-history
  [db]
  (:tool-history db))

(m/=> undos? [:-> App boolean?])
(defn undos?
  [db]
  (boolean (seq (:undos (tool-history db)))))

(m/=> redos? [:-> App boolean?])
(defn redos?
  [db]
  (boolean (seq (:redos (tool-history db)))))

(m/=> push-state [:-> App App])
(defn push-state
  [db]
  (let [elements (get-in db (element.handlers/path db))]
    (update db :tool-history
            #(assoc %
                    :undos (conj (:undos % []) elements)
                    :redos []))))

(m/=> clear-history [:-> App App])
(defn clear-history
  [db]
  (dissoc db :tool-history))

(m/=> set-state [:-> App State App])
(defn set-state
  [db state]
  (assoc db :state state))

(m/=> set-cursor [:-> App Cursor App])
(defn set-cursor
  [db cursor]
  (assoc db :cursor cursor))

(m/=> activate [:-> App Tool [:* any?] App])
(defn activate
  [db tool & {:as props}]
  (cond-> db
    :always
    (tool.hierarchy/on-deactivate)

    (and (not= (:cached-state db) :create)
         (not= (:state db) :type))
    (-> (history.handlers/reset-state)
        (clear-history))

    :always
    (-> (assoc :tool tool)
        (set-state :idle)
        (set-cursor "default")
        (dissoc :drag :pointer-offset :clicked-element)
        (snap.handlers/rebuild-tree)
        (tool.hierarchy/on-activate props))))

(m/=> deactivate [:-> App App])
(defn deactivate
  [db]
  (activate db ::tool.impl.base.transform/transform))

(m/=> edit [:-> App App])
(defn edit
  [db]
  (activate db ::tool.impl.base.edit/edit))

(m/=> pointer-delta [:-> App Vec2])
(defn pointer-delta
  [db]
  (utils.math/v-sub (:local-pointer-pos db)
                    (:local-pointer-offset db)))

(m/=> snapped-offset [:-> App Vec2])
(defn snapped-offset
  [db]
  (or (:nearest-neighbor-offset db)
      (:local-pointer-offset db)))

(m/=> snapped-position [:-> App Vec2])
(defn snapped-position
  [db]
  (or (:point (:nearest-neighbor db))
      (:local-pointer-pos db)))

(m/=> axis-pan-offset [:-> number? number? number? number?])
(defn axis-pan-offset
  [position offset size]
  (let [threshold (min (/ size 10) 100)
        step (min (/ size 50) 100)
        neg-step? (and (< position threshold) (< position offset))
        pos-step? (and (> position (- size threshold)) (> position offset))]
    (cond
      neg-step? (- step)
      pos-step? step
      :else 0)))

(m/=> pan-out-of-canvas [:-> App DomRect Vec2 Vec2 App])
(defn pan-out-of-canvas
  [db dom-rect pointer-pos pointer-offset]
  (let [[x y] pointer-pos
        [offset-x offset-y] pointer-offset
        pan [(axis-pan-offset x offset-x (:width dom-rect))
             (axis-pan-offset y offset-y (:height dom-rect))]]
    (cond-> db
      (not-every? zero? pan)
      (-> (frame.handlers/pan-by pan)
          ;; REVIEW: Can we improve performance?
          (snap.handlers/update-viewport-tree)))))

(m/=> set-cached [:-> App Tool [:* any?] App])
(defn set-cached
  [db tool & {:as props}]
  (cond-> db
    (not= tool (:tool db))
    (-> (assoc :cached-tool (:tool db)
               :cached-state (:state db))
        (activate tool props))))

(m/=> reset-cached [:-> App App])
(defn reset-cached
  [db]
  (let [{:keys [cached-tool cached-state]} db]
    (cond-> db
      (and cached-tool cached-state)
      (-> (activate cached-tool)
          (set-state cached-state)
          (dissoc :cached-tool :cached-state)))))

(m/=> cancel [:-> App App])
(defn cancel
  [db]
  (cond-> db
    :always
    (-> (activate (:tool db))
        (history.handlers/reset-state))

    (= (:state db) :idle)
    (deactivate)

    (:cached-tool db)
    (reset-cached)

    :always
    (-> (input.handlers/clear-pointer-data)
        (set-state :idle))))

(m/=> select-box [:-> App Element])
(defn select-box
  [db]
  (let [zoom (get-in db [:documents (:active-document db) :zoom])
        [pos-x pos-y] (:local-pointer-pos db)
        [offset-x offset-y] (:local-pointer-offset db)]
    {:tag :rect
     :attrs {:x (str (min pos-x offset-x))
             :y (str (min pos-y offset-y))
             :width (str (abs (- pos-x offset-x)))
             :height (str (abs (- pos-y offset-y)))
             :shape-rendering "crispEdges"
             :fill-opacity ".1"
             :fill "var(--accent)"
             :stroke "var(--accent)"
             :stroke-opacity ".5"
             :stroke-width (str (/ 1 zoom))}}))

(m/=> set-select-box [:-> App [:maybe Element] App])
(defn set-select-box
  [db el]
  (assoc db :select-box el))

(m/=> undo [:-> App App])
(defn undo
  [db]
  (let [{:keys [undos redos]} (tool-history db)
        current-state (get-in db (element.handlers/path db))]
    (if-let [previous-state (peek undos)]
      (-> db
          (assoc-in (element.handlers/path db) previous-state)
          (assoc :tool-history {:undos (pop undos)
                                :redos (conj redos current-state)}))
      (cancel db))))

(m/=> redo [:-> App App])
(defn redo
  [db]
  (let [{:keys [undos redos]} (tool-history db)
        current-state (get-in db (element.handlers/path db))]
    (if-let [next-elements (peek redos)]
      (-> db
          (assoc-in (element.handlers/path db) next-elements)
          (assoc :tool-history {:undos (conj undos current-state)
                                :redos (pop redos)}))
      (cancel db))))
