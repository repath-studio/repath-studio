(ns renderer.tool.impl.misc.guide
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.action.events :as-alias action.events]
   [renderer.app.handlers :as app.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.input.handlers :as input.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]))

(hierarchy/derive! ::guide ::tool.hierarchy/tool)

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

(defmethod tool.hierarchy/help [::guide :idle]
  []
  (i18n.views/t [::idle "Drag the ruler to the canvas to create a guide."]))

(defmethod tool.hierarchy/help [::guide :create]
  []
  (i18n.views/t [::create "Drop to finalize the guide."]))

(defmethod tool.hierarchy/on-activate ::guide
  [db & {:as props}]
  (let [{:keys [orientation]} props
        orientation (or orientation @orient)]
    (if
     orientation
      (-> db
          (assoc :guides true)
          (assoc :guides-locked false)
          (tool.handlers/set-cursor (cursor orientation))
          (app.handlers/add-fx [::set-orientation orientation]))
      (tool.handlers/deactivate db))))

(defmethod tool.hierarchy/on-pointer-move [::guide :idle]
  [db {:keys [pointer-id]
       :as e}]
  (-> db
      (history.handlers/reset-state)
      (assoc-in [:active-pointers pointer-id] e)
      (input.handlers/on-drag-start e)))

(defmethod tool.hierarchy/on-pointer-move [::guide :create]
  [db _e]
  (let [[x y] (tool.handlers/snapped-position db)]
    (-> db
        (element.handlers/update-selected #(assoc-in % [:attrs :x] (str x)))
        (element.handlers/update-selected #(assoc-in % [:attrs :y] (str y))))))

(defmethod tool.hierarchy/on-pointer-down [::guide :create]
  [db _e]
  (let [[x y] (tool.handlers/snapped-position db)]
    (element.handlers/add db {:type :element
                              :tag :guide
                              :attrs {:x x
                                      :y y
                                      :orientation (name @orient)}})))

(defmethod tool.hierarchy/on-drag-start [::guide :idle]
  [db e]
  (-> db
      (tool.handlers/set-state :create)
      (tool.hierarchy/on-pointer-down e)))

(defn finalize
  [db e]
  (-> db
      (history.handlers/finalize (:timestamp e) [::add-guide "Add guide"])
      (tool.handlers/deactivate)))

(defmethod tool.hierarchy/on-pointer-up [::guide :create]
  [db e]
  (finalize db e))

(defmethod tool.hierarchy/on-drag-end [::guide :create]
  [db e]
  (finalize db e))

(defmethod tool.hierarchy/snapping-points [::guide :create]
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label [::guide-position "guide position"]})])

(defmethod tool.hierarchy/snapping-elements [::guide :create]
  [db]
  (element.handlers/visible db))

(rf/dispatch [::action.events/register-action
              {:id :tool/guide
               :label [::label "Guide"]
               :icon "ruler-straight"
               :event [::tool.events/activate ::guide]
               :active [::tool.subs/active? ::guide]}])
