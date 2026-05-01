(ns hooks
  (:require
   [clj-kondo.hooks-api :as api]))

(defn ^:export => [{:keys [node]}]
  (let [[macro-sym name-node & schema-nodes] (:children node)
        name-with-meta (with-meta name-node {:clj-kondo/ignore-reference true})]
    {:node (api/list-node (list* macro-sym name-with-meta schema-nodes))}))
