(ns renderer.utils.length
  (:require
   [clojure.string :as string]
   [malli.core :as m]
   [renderer.element.db :refer [ElementTag]]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.unit :as utils.unit]))

(def ppi 96)

;; TODO: Find an agnostic way to handle percentages and em (we need to a base).
(def units
  {"px" 1
   "ch" 8
   "ex" 7.15625
   "em" 16
   "rem" 16
   "in" ppi
   "cm" (/ ppi 2.54)
   "mm" (/ ppi 25.4)
   "pt" (/ ppi 72)
   "pc" (/ ppi 6)
   "%" 1})

(m/=> valid-unit? [:-> string? boolean?])
(defn valid-unit?
  [s]
  (contains? units s))

(m/=> multiplier [:-> string? number?])
(defn multiplier
  "Returns the multiplier by unit.
   If the unit is invalid, it fallbacks to px (1)."
  [s]
  (or (get units (string/lower-case s))
      1))

(m/=> ->px [:-> number? string? number?])
(defn ->px
  [n unit]
  (* n (multiplier unit)))

(m/=> ->unit [:-> number? string? number?])
(defn ->unit
  [n unit]
  (/ n (multiplier unit)))

(m/=> unit->px [:function
                [:-> [:or string? number? nil?] number?]
                [:-> [:or string? number? nil?] number? number?]
                [:-> [:or string? number? nil?] ElementTag keyword? number?]])
(defn unit->px
  ([v]
   (unit->px v 0))
  ([v initial]
   (let [[n unit] (utils.unit/parse v)]
     (if (empty? unit)
       n
       (if (valid-unit? unit)
         (->px n unit)
         initial))))
  ([v tag attr]
   (let [initial (utils.attribute/initial-memo tag attr)]
     (unit->px v (unit->px initial 0)))))

(m/=> ->fixed [:function
               [:-> number? string?]
               [:-> number? integer? string?]
               [:-> number? integer? boolean? string?]])
(defn ->fixed
  ([v]
   (->fixed v 3))
  ([v precision]
   (->fixed v precision true))
  ([v precision remove-trailing-zeros]
   (cond-> (.toFixed v precision)
     remove-trailing-zeros
     (-> js/parseFloat str))))

(m/=> transform [:-> [:or string? number? nil?] ifn? [:* any?] string?])
(defn transform
  "Converts a value to pixels, applies a function and converts the result
   back to the original unit."
  ([v f & more]
   (let [[n unit] (utils.unit/parse v)]
     (-> (apply f (->px n unit) more)
         (->fixed)
         (js/parseFloat)
         (->unit unit)
         (str (when (valid-unit? unit) unit))))))
