(ns renderer.utils.clock
  (:require
   [clojure.string :as string]
   [malli.core :as m]))

(def metrics
  ["h" "min" "s" "ms"])

(def metrics-multiplier
  {"h" (* 1000 60 60)
   "min" (* 1000 60)
   "s" 1000
   "ms" 1})

(def timecount-pattern
  #"^(\d+\.?\d*)(h|min|s|ms)?$")

(m/=> index->multiplier [:-> int? number?])
(defn index->multiplier
  [i]
  (get metrics-multiplier (get metrics i)))

(m/=> timecount->ms [:-> string? [:maybe number?]])
(defn timecount->ms
  [s]
  (when-let [[_ number unit] (re-find timecount-pattern s)]
    (* (max 0 (js/parseFloat number))
       (or (get metrics-multiplier unit)
           (get metrics-multiplier "s")))))

(def full-clock-count (dec (count metrics)))

(m/=> clock->ms [:-> vector? [:maybe number?]])
(defn clock->ms
  [clock]
  (when (<= (count clock) full-clock-count)
    (let [start (- full-clock-count (count clock))]
      (reduce-kv (fn [total index timecount]
                   (+ total (* (max 0 (js/parseFloat timecount))
                               (index->multiplier (+ start index)))))
                 0
                 clock))))

(m/=> ->ms [:-> string? [:maybe number?]])
(defn ->ms
  [s]
  (let [clock (-> (string/trim s)
                  (string/split #":"))]
    (when (seq clock)
      (if (next clock)
        (clock->ms clock)
        (timecount->ms (first clock))))))
