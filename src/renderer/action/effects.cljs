(ns renderer.action.effects
  (:require
   [re-frame.core :as rf]
   [re-pressed.core :as-alias re-pressed]
   [renderer.utils.key :as utils.key]))

(def prevent-default-keys
  [{:keyCode (utils.key/codes "EQUALS")}
   {:keyCode (utils.key/codes "DASH")}
   {:keyCode (utils.key/codes "RIGHT")}
   {:keyCode (utils.key/codes "LEFT")}
   {:keyCode (utils.key/codes "UP")}
   {:keyCode (utils.key/codes "DOWN")}
   {:keyCode (utils.key/codes "F1")}
   {:keyCode (utils.key/codes "F11")}
   {:keyCode (utils.key/codes "F")
    :altKey true}
   {:keyCode (utils.key/codes "E")
    :altKey true}
   {:keyCode (utils.key/codes "A")
    :ctrlKey true}
   {:keyCode (utils.key/codes "O")
    :ctrlKey true}
   {:keyCode (utils.key/codes "S")
    :ctrlKey true}
   {:keyCode (utils.key/codes "G")
    :ctrlKey true}
   {:keyCode (utils.key/codes "P")
    :ctrlKey true}
   {:keyCode (utils.key/codes "W")
    :ctrlKey true}
   {:keyCode (utils.key/codes "K")
    :ctrlKey true}
   {:keyCode (utils.key/codes "W")
    :ctrlKey true}
   {:keyCode (utils.key/codes "D")
    :ctrlKey true
    :shiftKey true}
   {:keyCode (utils.key/codes "T")
    :ctrlKey true
    :shiftKey true}])

(defn shortcut-modifier-count
  [shortcut]
  (->> [:ctrlKey :shiftKey :altKey]
       (filter shortcut)
       (count)))

(defn max-modifier-count
  [action]
  (->> action
       :shortcuts
       (map shortcut-modifier-count)
       (apply max 0)))

(defn actions->keydown-rules
  [actions]
  {:event-keys (->> actions
                    (filter :shortcuts)
                    (sort-by max-modifier-count >)
                    (mapv (fn [{:keys [event shortcuts]}]
                            (into [event] (map vector shortcuts)))))
   :clear-keys []
   :always-listen-keys []
   :prevent-default-keys prevent-default-keys})

(rf/reg-fx
 ::update-keydown-rules
 (fn [actions]
   (->> actions
        (actions->keydown-rules)
        (conj [::re-pressed/set-keydown-rules])
        (rf/dispatch))))
