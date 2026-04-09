(ns renderer.toolbar.db
  (:require
   [renderer.action.db :refer [ActionId]]
   [renderer.db :refer [Orientation]]))

(def Toolbar
  [:map {:closed true}
   [:orientation Orientation]
   [:actions [:vector ActionId]]])
