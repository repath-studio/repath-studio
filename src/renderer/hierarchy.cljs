(ns renderer.hierarchy)

(defonce hierarchy (atom (make-hierarchy)))

(defn derive!
  [k parent]
  (swap! hierarchy derive k parent))
