(ns renderer.shell.hierarchy
  (:require
   [renderer.hierarchy :as hierarchy]))

(defmulti init identity :hierarchy hierarchy/hierarchy)
(defmulti help identity :hierarchy hierarchy/hierarchy)
(defmulti evaluate identity :hierarchy hierarchy/hierarchy)
(defmulti completion identity :hierarchy hierarchy/hierarchy)
(defmulti codemirror-options identity :hierarchy hierarchy/hierarchy)

(defmethod init :default [_language _params])
(defmethod help :default [_language])
(defmethod evaluate :default [_language _s])
(defmethod completion :default [_language _s])
(defmethod codemirror-options :default [_language])


