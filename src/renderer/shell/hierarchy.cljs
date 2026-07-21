(ns renderer.shell.hierarchy
  (:require
   [renderer.hierarchy :as hierarchy]))

(defmulti init :language :hierarchy hierarchy/hierarchy)
(defmulti welcome identity :hierarchy hierarchy/hierarchy)
(defmulti help identity :hierarchy hierarchy/hierarchy)
(defmulti evaluate identity :hierarchy hierarchy/hierarchy)
(defmulti show-error identity :hierarchy hierarchy/hierarchy)
(defmulti completion identity :hierarchy hierarchy/hierarchy)
(defmulti docs identity :hierarchy hierarchy/hierarchy)
(defmulti codemirror-options identity :hierarchy hierarchy/hierarchy)

(defmethod init :default [_params])
(defmethod welcome :default [_language])
(defmethod help :default [_language _command])
(defmethod evaluate :default [_language _s])
(defmethod show-error :default [_language _error])
(defmethod completion :default [_language _s])
(defmethod docs :default [_language _s])
(defmethod codemirror-options :default [_language])


