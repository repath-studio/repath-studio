(ns renderer.tool.impl.base.edit.select
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.db :refer [App]]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.input.db :refer [PointerEvent]]
   [renderer.tool.db :refer [Handle]]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.edit.core :as-alias edit]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.bounds :as utils.bounds]))

(m/=> hovered? [:-> Handle boolean? boolean?])
(defn hovered?
  [handle]
  (if-let [selection-bbox (-> @(rf/subscribe [::tool.subs/select-box])
                              (element.hierarchy/bbox))]
    (utils.bounds/contained-point? selection-bbox (:position handle))
    false))

(m/=> reduce-by-area [:-> App PointerEvent ifn? App])
(defn reduce-by-area
  [db f]
  (->> (element.handlers/handles db)
       (transduce (filter #(and (hovered? %)
                                (not (:rounded %))))
                  (fn [db handle] (cond-> db
                                    (:id handle)
                                    (f (:id handle) (:element-id handle))))
                  db)))

(defmethod tool.hierarchy/on-drag [::edit/edit :select]
  [db _e]
  (-> db
      (element.handlers/clear-hovered)
      (tool.handlers/set-select-box (tool.handlers/select-box db))
      (reduce-by-area element.handlers/hover)))

(defmethod tool.hierarchy/on-drag-end [::edit/edit :select]
  [db e]
  (cond-> db
    (not (:shift-key e))
    (element.handlers/assoc-prop :selected-handles #{})

    :always
    (-> (reduce-by-area element.handlers/select-handle)
        (tool.handlers/set-select-box nil)
        (tool.handlers/set-state :idle)
        (history.handlers/finalize (.-timeStamp e)
                                   [::select-handles "Select handles"]))))
