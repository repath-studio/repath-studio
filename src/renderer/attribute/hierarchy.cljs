(ns renderer.attribute.hierarchy
  (:require
   [renderer.hierarchy :as hierarchy]))

(defn dispatch [tag k & _more] [tag k])

(defmulti initial dispatch :hierarchy hierarchy/hierarchy)
(defmulti description dispatch :hierarchy hierarchy/hierarchy)
(defmulti form-element dispatch :hierarchy hierarchy/hierarchy)
(defmulti update-attr
  (fn [el k & _more] [(:tag el) k])
  :hierarchy hierarchy/hierarchy)

(defmethod initial :default [_tag _k] nil)
(defmethod description :default [_tag _k] nil)
(defmethod update-attr :default
  ([el k f]
   (update-in el [:attrs k] f))
  ([el k f arg]
   (update-in el [:attrs k] f arg))
  ([el k f arg & more]
   (apply update-in el [:attrs k] f arg more)))
