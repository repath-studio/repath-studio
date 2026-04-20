(ns renderer.tool.impl.misc.guide
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.app.handlers :as app.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.input.handlers :as input.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(tool.hierarchy/derive-tool :guide ::tool.hierarchy/tool)

(defonce orient (reagent/atom nil))

(rf/reg-fx
 ::set-orientation
 (fn [value]
   (reset! orient value)))

(defn cursor
  [orientation]
  (if (= orientation :horizontal)
    "ns-resize"
    "ew-resize"))

(defmethod tool.hierarchy/help [:guide :idle]
  []
  (i18n.views/t [::help "Drag the ruler to the canvas to create a guide."]))

(defmethod tool.hierarchy/help [:guide :create]
  []
  (i18n.views/t [::help "Drop to finalize the guide."]))

(defmethod tool.hierarchy/on-activate :guide
  [db & {:as props}]
  (let [{:keys [orientation]} props]
    (cond-> db
      orientation
      (-> (assoc :guides true)
          (assoc :guides-locked false)
          (tool.handlers/set-cursor (cursor orientation))
          (app.handlers/add-fx [::set-orientation orientation])))))

(defmethod tool.hierarchy/on-cancel :guide
  [db]
  (tool.handlers/activate db :transform))

(defmethod tool.hierarchy/on-pointer-move :guide
  [db {:keys [pointer-id]
       :as e}]
  (-> db
      (history.handlers/reset-state)
      (assoc-in [:active-pointers pointer-id] e)
      (input.handlers/on-drag-start e)))

(defmethod tool.hierarchy/on-pointer-down :guide
  [db _e]
  (let [[x y] (tool.handlers/snapped-position db)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :virtual true
                               :tag :guide
                               :attrs {:x x
                                       :y y
                                       :orientation (name @orient)}}))))

(defmethod tool.hierarchy/on-drag-start :guide
  [db e]
  (tool.hierarchy/on-pointer-down db e))

(defmethod tool.hierarchy/on-pointer-up :guide
  [db _e]
  (-> db
      (history.handlers/finalize (:timestamp db) [::add-guide "Add guide"])
      (tool.handlers/activate :transform)))

(defmethod tool.hierarchy/on-drag-end :guide
  [db _e]
  (-> db
      (history.handlers/finalize (:timestamp db) [::add-guide "Add guide"])
      (tool.handlers/activate :transform)))

(defmethod tool.hierarchy/on-drag :guide
  [db _e]
  (let [[x y] (tool.handlers/snapped-position db)]
    (print "a")
    (-> db
        (element.handlers/update-selected #(assoc-in % [:attrs :x] x))
        (element.handlers/update-selected #(assoc-in % [:attrs :y] y)))))

(defmethod tool.hierarchy/snapping-points :guide
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label [::guide-position "guide position"]})])

(defmethod tool.hierarchy/snapping-elements :guide
  [db]
  (element.handlers/visible db))
