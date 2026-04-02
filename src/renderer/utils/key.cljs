(ns renderer.utils.key
  (:require
   [clojure.set :as set]
   [malli.core :as m])
  (:import
   [goog.events KeyCodes]))

(m/=> arrow? [:-> string? boolean?])
(defn arrow?
  [k]
  (contains? #{"ArrowUp" "ArrowDown" "ArrowLeft" "ArrowRight"} k))

(defn down-handler!
  "Generic on-key-down handler for uncontrolled input elements that dispatches
   an event `f` in order to update a db value on keyboard enter, or reset to the
   initial value `v` on escape.

   We need uncontrolled inputs to avoid updating the canvas with incomplete
   values while the user is typing, and also avoid polluting the history stack.

   The `default-value` attribute should be used to update the value reactively."
  [e v f & more]
  (let [target (.-target e)]
    (.stopPropagation e)

    (case (.-key e)
      "Enter" (do (apply f e more)
                  (.blur target))
      "Escape" (do (set! (.-value target) v)
                   (.blur target))
      nil)))

(def codes
  "https://google.github.io/closure-library/api/goog.events.KeyCodes.html"
  (js->clj KeyCodes))

(def key-chars
  (set/map-invert codes))

(m/=> code->key [:-> number? [:maybe string?]])
(defn code->key
  [key-code]
  (get key-chars key-code))
