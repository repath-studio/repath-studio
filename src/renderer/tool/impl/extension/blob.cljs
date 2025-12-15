(ns renderer.tool.impl.extension.blob
  "Custom element for https://blobs.dev/"
  (:require
   [clojure.core.matrix :as matrix]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.length :as utils.length]))

(tool.hierarchy/derive-tool :blob ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :blob
  []
  {:icon "blob"
   :label [::label "Blob"]})

(defn pointer-delta
  [db]
  (matrix/distance (tool.handlers/snapped-position db)
                   (tool.handlers/snapped-offset db)))

(defn attributes
  [db]
  (let [[offset-x offset-y] (tool.handlers/snapped-offset db)
        radius (pointer-delta db)]
    {:x (utils.length/->fixed (- offset-x radius))
     :y (utils.length/->fixed (- offset-y radius))
     :size (utils.length/->fixed (* radius 2))}))

(defmethod tool.hierarchy/on-drag-start :blob
  [db _e]
  (let [fill (document.handlers/attr db :fill)
        stroke (document.handlers/attr db :stroke)
        seed (rand-int 1000000)]
    (-> (tool.handlers/set-state db :create)
        (element.handlers/add {:type :element
                               :tag :blob
                               :attrs (merge (attributes db)
                                             {:seed seed
                                              :extraPoints 8
                                              :randomness 4
                                              :fill fill
                                              :stroke stroke})}))))

(defmethod tool.hierarchy/on-drag :blob
  [db _e]
  (let [attrs (attributes db)
        assoc-attr (fn [el [k v]] (assoc-in el [:attrs k] (str v)))
        [min-x min-y] (element.handlers/parent-offset db)]
    (-> db
        (element.handlers/update-selected #(reduce assoc-attr % attrs))
        (element.handlers/translate [(- min-x) (- min-y)]))))

(defmethod tool.hierarchy/on-drag-end :blob
  [db _e]
  (-> db
      (history.handlers/finalize [::create-blob "Create blob"])
      (tool.handlers/activate :transform)))
