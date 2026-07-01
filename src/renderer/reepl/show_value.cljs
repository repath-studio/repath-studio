(ns renderer.reepl.show-value
  (:require
   [cljs.pprint :as pprint]
   [renderer.utils.extra :refer [rpartial]]))

(defn pprint-str
  [v]
  (pprint/write v :stream nil))

(defn show-str
  [v]
  (if (string? v)
    v
    (pprint-str v)))

(defn show-value-
  [v config showers]
  (loop [shower-list showers]
    (if (empty? shower-list)
      (throw (js/Error. (str "No shower for value " v)))
      (let [res (->> showers
                     (rpartial show-value-)
                     ((first shower-list) v config))]
        (if res
          [:div.inline-flex res]
          (recur (rest shower-list)))))))

(defn show-value [v opts show-opts]
  (show-value- v opts (conj (vec (:showers show-opts)) show-str)))
