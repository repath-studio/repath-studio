(ns renderer.a11y.handlers
  (:require
   [malli.core :as m]
   [malli.error :as m.error]
   [renderer.a11y.db :as a11y.db :refer [A11yFilter A11yFilterId]]
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
  (if-not (a11y.db/valid-filter? a11y-filter)
    (throw (ex-info (str "Invalid a11y filter: "
                         (-> (a11y.db/explain-filter a11y-filter)
                             (m.error/humanize)))
                    {:a11y-filter a11y-filter}))
    (-> db
        (deregister-filter (:id a11y-filter))
        (update-in [:a11y :filters] conj a11y-filter))))
