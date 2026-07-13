(ns renderer.element.hierarchy
  "Multimethods for rendering, editing, and manipulating elements.

   This is our element API, so new or custom element types should implement
   these multimethods. The dispatch function is based on the element's :tag.
   Arbitrary additional arguments can be passed to the multimethods, but the
   first argument is always the element, or the element's tag (identity).

   See the :default definitions for the additional arguments."
  (:require
   [renderer.hierarchy :as hierarchy]))

(defn dispatch
  [el & _more]
  (:tag el))

(defmulti render dispatch :hierarchy hierarchy/hierarchy)
(defmulti render-to-string dispatch :hierarchy hierarchy/hierarchy)
(defmulti render-edit dispatch :hierarchy hierarchy/hierarchy)
(defmulti handles dispatch :hierarchy hierarchy/hierarchy)
(defmulti path dispatch :hierarchy hierarchy/hierarchy)
(defmulti area dispatch :hierarchy hierarchy/hierarchy)
(defmulti centroid dispatch :hierarchy hierarchy/hierarchy)
(defmulti snapping-points dispatch :hierarchy hierarchy/hierarchy)
(defmulti bbox dispatch :hierarchy hierarchy/hierarchy)
(defmulti delete-segments dispatch :hierarchy hierarchy/hierarchy)
(defmulti translate dispatch :hierarchy hierarchy/hierarchy)
(defmulti scale dispatch :hierarchy hierarchy/hierarchy)
(defmulti handle-drag dispatch :hierarchy hierarchy/hierarchy)
(defmulti handle-click dispatch :hierarchy hierarchy/hierarchy)
(defmulti properties identity :hierarchy hierarchy/hierarchy)

(defmethod render :default [_el])
(defmethod render-to-string :default [el] [render el])
(defmethod render-edit :default [_el])
(defmethod handles :default [_el])
(defmethod bbox :default [_el])
(defmethod delete-segments :default [el] el)
(defmethod area :default [_el])
(defmethod centroid :default [_el])
(defmethod snapping-points :default [_el] [])
(defmethod translate :default [el _offset] el)
(defmethod scale :default [el _ratio _pivot-point] el)
(defmethod handle-drag :default [el _offset _handle _lock?] el)
(defmethod handle-click :default [el _handle] el)
(defmethod properties :default [_tag])

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
