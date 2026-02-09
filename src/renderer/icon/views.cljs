(ns renderer.icon.views
  (:require
   [re-frame.core :as rf]
   [renderer.icon.subs :as-alias icon.subs]
   [renderer.views :refer [merge-with-class]]))

(defn svg-icon
  [id props]
  (when-let [icon @(rf/subscribe [::icon.subs/icon id])]
    [:svg (merge-with-class {:class "fill-current"
                             :viewBox "0 0 17 17"
                             :width "17"
                             :height "17"}
                            props)
     [:path {:d (:icon-path icon)}]]))
