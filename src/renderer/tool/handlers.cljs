(ns renderer.tool.handlers
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [Vec2]]
   [renderer.frame.db :refer [DomRect]]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.input.handlers :as input.handlers]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.db :refer [Tool State Cursor]]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.edit :as-alias tool.impl.base.edit]
   [renderer.tool.impl.base.transform.core :as-alias tool.impl.base.transform]))

(m/=> set-state [:-> App State App])
(defn set-state
  [db state]
  (assoc db :state state))

(m/=> set-cursor [:-> App Cursor App])
(defn set-cursor
  [db cursor]
  (assoc db :cursor cursor))

(m/=> help [:-> Tool State any?])
(defn help
  [tool state]
  (tool.hierarchy/help tool state))

(m/=> activate [:-> App Tool [:* any?] App])
(defn activate
  [db tool & {:as props}]
  (cond-> db
    :always
    (tool.hierarchy/on-deactivate)

    (and (not= (:cached-state db) :create)
         (not= (:state db) :type))
    (history.handlers/reset-state)

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
  (matrix/sub (:adjusted-pointer-pos db)
              (:adjusted-pointer-offset db)))

(m/=> snapped-offset [:-> App Vec2])
(defn snapped-offset
  [db]
  (or (:nearest-neighbor-offset db)
      (:adjusted-pointer-offset db)))

(m/=> snapped-position [:-> App Vec2])
(defn snapped-position
  [db]
  (or (:point (:nearest-neighbor db))
      (:adjusted-pointer-pos db)))

(m/=> axis-pan-offset [:-> number? number? number? number?])
(defn axis-pan-offset
  [position offset size]
  (let [threshold (min (/ size 10) 100)
        step (min (/ size 50) 100)]
    (cond
      (and (< position threshold)
           (< position offset))
      (- step)

      (and (> position (- size threshold))
           (> position offset))
      step

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
          ; REVIEW: Can we improve performance?
          (snap.handlers/update-viewport-tree)))))

(defn set-cached
  [db tool]
  (-> db
      (assoc :cached-tool (:tool db)
             :cached-state (:state db))
      (activate tool)))

(defn reset-cached
  [db]
  (let [{:keys [cached-tool cached-state]} db]
    (-> db
        (activate cached-tool)
        (set-state cached-state)
        (dissoc :cached-tool :cached-state))))

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
