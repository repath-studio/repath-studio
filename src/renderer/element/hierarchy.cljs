(ns renderer.element.hierarchy
  (:require
   [renderer.hierarchy :as hierarchy]))

(hierarchy/derive! ::graphics ::renderable)
(hierarchy/derive! ::gradient ::renderable)
(hierarchy/derive! ::descriptive ::renderable)
(hierarchy/derive! :foreignObject ::graphics)
(hierarchy/derive! :textPath ::graphics)
(hierarchy/derive! :tspan ::graphics)
(hierarchy/derive! :linearGradient ::gradient)
(hierarchy/derive! :radialGradient ::gradient)
(hierarchy/derive! :desc ::descriptive)
(hierarchy/derive! :metadata ::descriptive)
(hierarchy/derive! :title ::descriptive)

(defmulti render :tag :hierarchy hierarchy/hierarchy)
(defmulti render-to-string :tag :hierarchy hierarchy/hierarchy)
(defmulti render-edit :tag :hierarchy hierarchy/hierarchy)
(defmulti path :tag :hierarchy hierarchy/hierarchy)
(defmulti area :tag :hierarchy hierarchy/hierarchy)
(defmulti centroid :tag :hierarchy hierarchy/hierarchy)
(defmulti snapping-points :tag :hierarchy hierarchy/hierarchy)
(defmulti bbox :tag :hierarchy hierarchy/hierarchy)

(defmulti translate
  (fn [el _offset] (:tag el))
  :hierarchy hierarchy/hierarchy)

(defmulti scale
  (fn [el _ratio _pivot-point] (:tag el))
  :hierarchy hierarchy/hierarchy)

(defmulti edit
  (fn [el _offset _handle _lock?] (:tag el))
  :hierarchy hierarchy/hierarchy)

(defmulti properties identity :hierarchy hierarchy/hierarchy)

(defmethod render :default [])
(defmethod render-to-string :default [el] [render el])
(defmethod render-edit :default [])
(defmethod bbox :default [])
(defmethod area :default [])
(defmethod centroid :default [])
(defmethod snapping-points :default [] [])
(defmethod translate :default [el _offset] el)
(defmethod scale :default [el _ratio _pivot-point] el)
(defmethod edit :default [el _offset _handle _lock?] el)
(defmethod properties :default [])
