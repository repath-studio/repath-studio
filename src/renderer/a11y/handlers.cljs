(ns renderer.a11y.handlers
  (:require
   [malli.core :as m]
   [renderer.a11y.db :refer [A11yFilter A11yFilterId]]
   [renderer.app.db :refer [App]]))

(m/=> deregister-filter [:-> App A11yFilterId App])
(defn deregister-filter
  [db a11y-filter-id]
  (update-in db [:a11y :filters]
             (fn [filters]
               (->> filters
                    (remove #(= (:id %) a11y-filter-id))
                    (into [])))))

(m/=> register-filter [:-> App A11yFilter App])
(defn register-filter
  [db a11y-filter]
  (-> db
      (deregister-filter (:id a11y-filter))
      (update-in [:a11y :filters] conj a11y-filter)))
