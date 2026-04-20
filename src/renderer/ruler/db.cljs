(ns renderer.ruler.db
  (:require
   [renderer.db :refer [Orientation]]))

(def Guide
  [:map {:closed true}
   [:type [:= :guide]]
   [:orientation Orientation]])
