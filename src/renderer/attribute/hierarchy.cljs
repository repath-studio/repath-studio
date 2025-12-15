(ns renderer.attribute.hierarchy)

(defonce hierarchy (atom (make-hierarchy)))

(defn derive-attribute
  [k parent]
  (swap! hierarchy derive k parent))

(defmulti initial (fn [tag k] [tag k]) :hierarchy hierarchy)
(defmulti update-attr (fn [_ k & _more] k) :hierarchy hierarchy)
(defmulti description (fn [tag k] [tag k]) :hierarchy hierarchy)
(defmulti form-element (fn [tag k _v _attrs] [tag k]) :hierarchy hierarchy)

(defmethod initial :default [_tag _k] nil)
(defmethod update-attr :default
  ([el k f]
   (update-in el [:attrs k] f))
  ([el k f arg]
   (update-in el [:attrs k] f arg))
  ([el k f arg & more]
   (apply update-in el [:attrs k] f arg more)))
