(ns renderer.utils.key
  (:require
   [clojure.set :as set]
   [malli.core :as m])
  (:import
   [goog.events KeyCodes]))

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

(defn shortcut-modifier-count
  [shortcut]
  (count (filter shortcut [:ctrlKey :shiftKey :altKey])))

(defn max-modifier-count
  [action]
  (apply max 0 (map shortcut-modifier-count (:shortcuts action))))

(defn actions->keydown-rules
  [actions]
  {:event-keys (->> actions
                    (filter :shortcuts)
                    (sort-by max-modifier-count >)
                    (mapv (fn [{:keys [event shortcuts]}]
                            (into [event] (map vector shortcuts)))))
   :clear-keys []
   :always-listen-keys []
   :prevent-default-keys [{:keyCode (codes "EQUALS")}
                          {:keyCode (codes "DASH")}
                          {:keyCode (codes "RIGHT")}
                          {:keyCode (codes "LEFT")}
                          {:keyCode (codes "UP")}
                          {:keyCode (codes "DOWN")}
                          {:keyCode (codes "F1")}
                          {:keyCode (codes "F11")}
                          {:keyCode (codes "F")
                           :altKey true}
                          {:keyCode (codes "E")
                           :altKey true}
                          {:keyCode (codes "A")
                           :ctrlKey true}
                          {:keyCode (codes "O")
                           :ctrlKey true}
                          {:keyCode (codes "S")
                           :ctrlKey true}
                          {:keyCode (codes "G")
                           :ctrlKey true}
                          {:keyCode (codes "P")
                           :ctrlKey true}
                          {:keyCode (codes "W")
                           :ctrlKey true}
                          {:keyCode (codes "K")
                           :ctrlKey true}
                          {:keyCode (codes "W")
                           :ctrlKey true}
                          {:keyCode (codes "D")
                           :ctrlKey true
                           :shiftKey true}]})
