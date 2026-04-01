(ns renderer.icon.views
  (:require
   [re-frame.core :as rf]
   [renderer.icon.subs :as-alias icon.subs]))

(defn path
  [id]
  (let [path-data @(rf/subscribe [::icon.subs/path-data id])]
    [:path {:d path-data}]))
