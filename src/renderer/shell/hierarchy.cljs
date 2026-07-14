(ns renderer.shell.hierarchy
  (:require
   [renderer.hierarchy :as hierarchy]))

(defmulti init identity :hierarchy hierarchy/hierarchy)
(defmulti help identity :hierarchy hierarchy/hierarchy)
(defmulti evaluate identity :hierarchy hierarchy/hierarchy)
(defmulti completion identity :hierarchy hierarchy/hierarchy)
(defmulti codemirror-mode identity :hierarchy hierarchy/hierarchy)

(defmethod init :default [_language])
(defmethod help :default [_language])
(defmethod evaluate :default [_language _s])
(defmethod completion :default [_language _s])
(defmethod codemirror-mode :default [_language])


