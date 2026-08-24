(ns renderer.utils.clock
  (:require
   [clojure.string :as string]))

(def metrics
  ["h" "min" "s" "ms"])

(def metrics-multiplier
  {"h" (* 1000 60 60)
   "min" (* 1000 60)
   "s" 1000
   "ms" 1})

(def timecount-pattern
  #"^(\d+\.?\d*)(h|min|s|ms)?$")

(defn index->multiplier
  [i]
  (get metrics-multiplier (get metrics i)))

(defn timecount->ms
  [t]
  (when-let [[_ number unit] (re-find timecount-pattern t)]
    (* (max 0 (js/parseFloat number))
       (or (get metrics-multiplier unit)
           (get metrics-multiplier "s")))))

(defn clock->ms
  [clock]
  (let [start (-> (count metrics)
                  (dec)
                  (- (count clock)))]
    (reduce-kv (fn [total index timecount]
                 (+ total (* (max 0 (js/parseFloat timecount))
                             (index->multiplier (+ start index)))))
               0
               clock)))

(defn ->ms
  [s]
  (let [clock (-> (string/trim s)
                  (string/split #":"))]
    (if (next clock)
      (clock->ms clock)
      (timecount->ms (first clock)))))
