(ns renderer.shell.hierarchy)

(defmulti init :language)
(defmulti welcome identity)
(defmulti help identity)
(defmulti evaluate identity)
(defmulti show-error identity)
(defmulti completions identity)
(defmulti docs identity)
(defmulti codemirror-options identity)

(defmethod init :default [_params])
(defmethod welcome :default [_language])
(defmethod help :default [_language _command])
(defmethod evaluate :default [_language _s])
(defmethod show-error :default [_language _error])
(defmethod completions :default [_language _s])
(defmethod docs :default [_language _s])
(defmethod codemirror-options :default [_language])


