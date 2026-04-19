(ns renderer.tool.impl.misc.guide
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.app.handlers :as app.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
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
    (if orientation
      (-> db
          (assoc :guides true)
          (element.handlers/set-guides-prop :visible true)
          (app.handlers/add-fx [::set-orientation orientation]))
      (tool.handlers/activate db :transform))))

(defmethod tool.hierarchy/on-deactivate :guide
  [db]
  (app.handlers/add-fx db [::set-orientation nil]))

(defmethod tool.hierarchy/on-drag-start :guide
  [db _e]
  (let [[x y] (tool.handlers/snapped-position db)]
    (-> db
        (tool.handlers/set-cursor (cursor @orient))
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :guide
                               :visible (:guides db)
                               :attrs {:x x
                                       :y y
                                       :orientation (name @orient)}}))))

(defmethod tool.hierarchy/on-drag :guide
  [db _e]
  (let [[x y] (tool.handlers/snapped-position db)]
    (-> db
        (element.handlers/update-selected #(assoc-in % [:attrs :x] x))
        (element.handlers/update-selected #(assoc-in % [:attrs :y] y)))))

(defmethod tool.hierarchy/on-drag-end :guide
  [db e]
  (let [{:keys [timestamp]} e]
    (cond-> db
      (:guides-locked db)
      (element.handlers/assoc-prop :locked true)

      :always
      (-> (history.handlers/finalize timestamp [::create-guide "Create guide"])
          (tool.handlers/activate :transform)))))

(defmethod tool.hierarchy/snapping-points :guide
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label [::guide-position "guide position"]})])

(defmethod tool.hierarchy/snapping-elements :guide
  [db]
  (element.handlers/visible db))
