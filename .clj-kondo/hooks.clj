(ns hooks
  (:require
   [clj-kondo.hooks-api :as api]))

(def declared (atom {}))

(defn handlers-ns? [ns-str]
  (and ns-str (re-find #"\.handlers$" (name ns-str))))

(defn ^:export => [{:keys [node]
                    :as args}]
  (let [ns-str (str (:ns args))
        [macro-sym name-node & schema-nodes] (:children node)
        name-str (str (api/sexpr name-node))
        name-with-meta (with-meta name-node {:clj-kondo/ignore-reference true})]
    (when (handlers-ns? ns-str)
      (swap! declared (fn [m] (assoc m ns-str (conj (m ns-str #{}) name-str)))))
    {:node (api/list-node (list* macro-sym name-with-meta schema-nodes))}))

(defn ^:export handler-defn [{:keys [node]
                              :as args}]
  (let [ns-str (str (:ns args))
        [_ name-node & _] (:children node)
        name-str (str (api/sexpr name-node))]
    (when (and (handlers-ns? ns-str)
               (not (get-in @declared [ns-str name-str])))
      (api/reg-finding!
       (assoc (meta node)
              :message (str name-str " is missing a schema declaration")
              :type :hooks/handler-missing-schema)))))
