(ns renderer.input.hierarchy)

(defn dispatch [_db e] (:type e))

(defmulti keyboard dispatch)
(defmulti pointer dispatch)
(defmulti wheel dispatch)
(defmulti drag dispatch)

(defmethod keyboard :default [db _e] db)
(defmethod pointer :default [db _e] db)
(defmethod drag :default [db _e] db)
