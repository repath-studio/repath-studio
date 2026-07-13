(ns renderer.reepl.hierarchy
  (:require
   [renderer.hierarchy :as hierarchy]))

(defmulti init identity :hierarchy hierarchy/hierarchy)
(defmulti eval-str identity :hierarchy hierarchy/hierarchy)

(defmethod init :default [_language] nil)
(defmethod eval-str :default [_language _s] nil)


