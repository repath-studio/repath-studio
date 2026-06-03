(ns renderer.tool.impl.base.edit.select
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.db :refer [Handle]]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.edit.core :as-alias edit]
   [renderer.utils.bounds :as utils.bounds]))

(m/=> selectable? [:-> App Handle boolean?])
(defn selectable?
  [db handle]
  (and (not (:rounded handle))
       (:select-box db)
       (some-> (:select-box db)
               (element.hierarchy/bbox)
               (utils.bounds/contained-point? (:position handle)))))

(m/=> reduce-by-area [:-> App ifn? App])
(defn reduce-by-area
  [db f]
  (->> (element.handlers/handles db)
       (transduce (filter (partial selectable? db))
                  (fn [db handle] (cond-> db
                                    (:id handle)
                                    (f (:id handle) (:parent handle))))
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
        (history.handlers/finalize (:timestamp e)
                                   [::select-handles "Select handles"]))))
