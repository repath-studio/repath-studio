(ns renderer.reepl.hierarchy
  (:require
   [renderer.hierarchy :as hierarchy]))

(defmulti init identity :hierarchy hierarchy/hierarchy)
(defmulti evaluate identity :hierarchy hierarchy/hierarchy)

(defmethod init :default [_language] nil)
(defmethod evaluate :default [_language _s] nil)


