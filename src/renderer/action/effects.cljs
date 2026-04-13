(ns renderer.action.effects
  (:require
   [re-frame.core :as rf]
   [re-pressed.core :as-alias re-pressed]))

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
   ;; Prevent default for all registered shortcuts to avoid conflicts with
   ;; browser shortcuts. I can't think of a case where we shouldn't do this.
   ;; In certain cases, the user might have to enter fullscreen mode to use some
   ;; shortcuts.
   :prevent-default-keys (->> actions
                              (keep :shortcuts)
                              (apply concat))})

(rf/reg-fx
 ::update-keydown-rules
 (fn [actions]
   (->> actions
        (actions->keydown-rules)
        (conj [::re-pressed/set-keydown-rules])
        (rf/dispatch))))
