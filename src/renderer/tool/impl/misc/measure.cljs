(ns renderer.tool.impl.misc.measure
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.element.handlers :as element.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.key :as utils.key]))

(tool.hierarchy/derive-tool :measure ::tool.hierarchy/tool)

(defmethod tool.hierarchy/help [:measure :idle]
  []
  (i18n.views/t [::help "Click and drag to measure a distance."]))

(defmethod tool.hierarchy/on-activate :measure
  [db]
  (tool.handlers/set-cursor db "crosshair"))

(defmethod tool.hierarchy/on-drag-start :measure
  [db _e]
  (let [[offset-x offset-y] (tool.handlers/snapped-offset db)
        [x y] (tool.handlers/snapped-position db)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :virtual true
                               :tag :measure
                               :attrs {:x1 offset-x
                                       :y1 offset-y
                                       :x2 x
                                       :y2 y}}))))

(defmethod tool.hierarchy/on-drag :measure
  [db _e]
  (let [[x y] (tool.handlers/snapped-position db)]
    (-> db
        (element.handlers/update-selected #(assoc-in % [:attrs :x2] x))
        (element.handlers/update-selected #(assoc-in % [:attrs :y2] y)))))

(defmethod tool.hierarchy/on-drag-end :measure
  [db _e]
  (tool.handlers/set-state db :idle))

(defmethod tool.hierarchy/snapping-points :measure
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label (if (= (:state db) :create)
               [::measure-end "measure end"]
               [::measure-start "measure start"])})])

(defmethod tool.hierarchy/snapping-elements :measure
  [db]
  (element.handlers/visible db))

(rf/dispatch [::action.events/register-action
              {:id :tool/measure
               :label [::label "Measure"]
               :icon "ruler-triangle"
               :event [::tool.events/activate :measure]
               :active [::tool.subs/active? :measure]
               :shortcuts [{:keyCode (utils.key/codes "M")}]}])
