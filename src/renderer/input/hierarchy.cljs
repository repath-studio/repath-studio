(ns renderer.input.hierarchy)

(defmulti keyboard (fn [_db e] (:type e)))
(defmulti pointer (fn [_db e] (:type e)))
(defmulti wheel (fn [_db e] (:type e)))
(defmulti drag (fn [_db e] (:type e)))

(defmethod keyboard :default [db _e] db)
(defmethod pointer :default [db _e] db)
(defmethod drag :default [db _e] db)
