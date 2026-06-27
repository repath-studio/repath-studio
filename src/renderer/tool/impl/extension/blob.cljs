(ns renderer.tool.impl.extension.blob
  "Custom element for https://blobs.dev/"
  (:require
   [clojure.core.matrix :as matrix]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.length :as utils.length]))

(hierarchy/derive! ::blob ::tool.hierarchy/element)

(defn pointer-delta
  [db]
  (matrix/distance (tool.handlers/snapped-position db)
                   (tool.handlers/snapped-offset db)))

(defn attributes
  [db]
  (let [[offset-x offset-y] (tool.handlers/snapped-offset db)
        radius (pointer-delta db)
        attrs (-> (document.handlers/attrs db)
                  (select-keys [:stroke :fill :stroke-width]))]
    (merge attrs {:x (utils.length/->fixed (- offset-x radius))
                  :y (utils.length/->fixed (- offset-y radius))
                  :size (utils.length/->fixed (* radius 2))})))

(defmethod tool.hierarchy/on-drag-start [::blob :idle]
  [db _e]
  (let [seed (rand-int 1000000)]
    (-> (tool.handlers/set-state db :create)
        (element.handlers/add {:type :element
                               :tag :blob
                               :attrs (merge (attributes db)
                                             {:seed seed
                                              :extraPoints 8
                                              :randomness 4})}))))

(defmethod tool.hierarchy/on-drag [::blob :create]
  [db _e]
  (let [attrs (attributes db)
        assoc-attr (fn [el [k v]] (assoc-in el [:attrs k] (str v)))
        [min-x min-y] (element.handlers/parent-offset db)]
    (-> db
        (element.handlers/update-selected #(reduce assoc-attr % attrs))
        (element.handlers/translate [(- min-x) (- min-y)]))))

(defmethod tool.hierarchy/on-drag-end [::blob :create]
  [db e]
  (-> db
      (history.handlers/finalize (:timestamp e) [::create-blob "Create blob"])
      (tool.handlers/deactivate)))
