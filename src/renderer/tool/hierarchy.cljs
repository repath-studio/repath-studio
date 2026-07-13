(ns renderer.tool.hierarchy
  "Multimethods that define the tool interactions on the canvas.

   This is our tool API, so new tools should implement these multimethods.
   The dispatch function is based on db :tool and :state. Arbitrary additional
   arguments can be passed to the multimethods, but the first argument is always
   the db, or a direct [tool state] vector.

   See the :default definitions for the input arguments. For input related 
   events, the second argument is always the event map."
  (:require
   [renderer.hierarchy :as hierarchy]))

(defn dispatch
  [db & _more]
  [(:tool db) (:state db)])

(defmulti on-pointer-down dispatch :hierarchy hierarchy/hierarchy)
(defmulti on-pointer-up dispatch :hierarchy hierarchy/hierarchy)
(defmulti on-pointer-move dispatch :hierarchy hierarchy/hierarchy)
(defmulti on-double-click dispatch :hierarchy hierarchy/hierarchy)
(defmulti on-drag-start dispatch :hierarchy hierarchy/hierarchy)
(defmulti on-context-menu dispatch :hierarchy hierarchy/hierarchy)
(defmulti on-drag dispatch :hierarchy hierarchy/hierarchy)
(defmulti on-drag-end dispatch :hierarchy hierarchy/hierarchy)
(defmulti on-key-up dispatch :hierarchy hierarchy/hierarchy)
(defmulti on-key-down dispatch :hierarchy hierarchy/hierarchy)
(defmulti snapping-points dispatch :hierarchy hierarchy/hierarchy)
(defmulti snapping-elements dispatch :hierarchy hierarchy/hierarchy)
(defmulti on-delete dispatch :hierarchy hierarchy/hierarchy)
(defmulti on-activate :tool :hierarchy hierarchy/hierarchy)
(defmulti on-deactivate :tool :hierarchy hierarchy/hierarchy)
(defmulti tool-options identity :hierarchy hierarchy/hierarchy)
(defmulti render identity :hierarchy hierarchy/hierarchy)
(defmulti attributes-panel identity :hierarchy hierarchy/hierarchy)
(defmulti help (fn [tool state] [tool state]) :hierarchy hierarchy/hierarchy)

(defmethod on-pointer-down :default [db _e] db)
(defmethod on-pointer-up :default [db _e] db)
(defmethod on-pointer-move :default [db _e] db)
(defmethod on-double-click :default [db _e] db)
(defmethod on-drag-start :default [db _e] db)
(defmethod on-drag :default [db _e] db)
(defmethod on-drag-end :default [db _e] db)
(defmethod on-context-menu :default [db _e] db)
(defmethod on-key-up :default [db _e] db)
(defmethod on-key-down :default [db _e] db)
(defmethod snapping-points :default [_db])
(defmethod snapping-elements :default [_db])
(defmethod on-delete :default [db] db)
(defmethod on-activate :default [db & {:as _props}] db)
(defmethod on-deactivate :default [db] db)
(defmethod tool-options :default [_tag])
(defmethod render :default [_tag])
(defmethod help :default [_tool _state])
