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
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]))

(m/=> selectable? [:-> App Handle boolean?])
(defn selectable?
  [db handle]
  (and (:select-box db)
       (let [{:keys [position parent]} handle
             position (-> (element.handlers/transform db parent)
                          (utils.element/transform-point position))]
         (-> (element.hierarchy/bbox (:select-box db))
             (utils.bounds/contained-point? position)))))

(m/=> reduce-by-area [:-> App ifn? App])
(defn reduce-by-area
  [db f]
  (->> (element.handlers/handles db)
       (transduce (filter (partial selectable? db))
                  (fn [db handle] (cond-> db handle (f handle)))
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
    (-> (reduce-by-area (fn [db handle]
                          (let [{:keys [id parent]} handle]
                            (element.handlers/select-handle db id parent))))
        (tool.handlers/set-select-box nil)
        (tool.handlers/set-state :idle)
        (history.handlers/finalize (:timestamp e)
                                   [::select-handles "Select handles"]))))
