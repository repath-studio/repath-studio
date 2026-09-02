(ns renderer.shell.reepl.codemirror
  (:require
   [clojure.string :as string]
   [renderer.utils.codemirror :as utils.codemirror]))

(defn should-eval?
  [inst evt]
  (or (.-metaKey evt)
      (and (not (.-shiftKey evt))
           (utils.codemirror/in-place? inst))))

(defn repl-hint
  [complete-word ^js inst _options]
  (when-let [result (utils.codemirror/current-word inst)]
    (let [from (.-from result)
          to (.-to result)
          text (.sliceDoc (.-state inst) from to)
          words (when-not (empty? text)
                  (->> (complete-word text)
                       ;; Remove core duplicates
                       (remove #(string/includes? (second %) "cljs.core"))
                       (vec)))]
      (when-not (empty? words)
        {:words words
         :num (count words)
         :active (= (get (first words) 2) text)
         :show-all false
         :initial-text text
         :pos 0
         :from from
         :to to}))))

(defn cycle-pos
  "Cycle through positions. Returns [active new-pos]."
  [n current-pos go-back? initial-active?]
  (if go-back?
    (if (>= 0 current-pos)
      (if initial-active?
        [true (dec n)]
        [false 0])
      [true (dec current-pos)])
    (if (>= current-pos (dec n))
      [initial-active? 0]
      [true (inc current-pos)])))

(defn should-cycle?
  [{:keys [words initial-text]
    :as state}]
  (and state
       (or (< 1 (count words))
           (and (< 0 (count words))
                (not= initial-text (get (first words) 2))))))

(defn cycle-completions
  [{:keys [num pos active from to words initial-text]
    :as state}
   go-back? inst evt]
  (when (should-cycle? state)
    (.preventDefault evt)
    (let [initial-active (= initial-text (get (first words) 2))
          [active pos] (if active
                         (cycle-pos num pos go-back? initial-active)
                         [true (if go-back? (dec num) pos)])
          text (if active
                 (get (get words pos) 2)
                 initial-text)]
      (.dispatch inst #js {:changes #js {:from from
                                         :to to
                                         :insert text}})
      (assoc state
             :pos pos
             :active active
             :to (+ from (count text))))))

(defn on-keyup-handler
  [options evt inst]
  (let [{:keys [complete-atom complete-word]} options]
    (.stopPropagation evt)
    (case (.-key evt)
      "Escape"
      (if @complete-atom
        (reset! complete-atom nil)
        (some-> (.-activeElement js/document)
                (.blur)))

      "Enter"
      (reset! complete-atom nil)

      ("Control" "Alt" "Meta" "ContextMenu")
      (swap! complete-atom assoc :show-all false)

      (when-not (contains? #{"Tab" "Shift"} (.-key evt))
        (reset! complete-atom (repl-hint complete-word inst nil))))))

(defn on-keydown-handler
  [options evt inst]
  (let [{:keys [complete-atom on-eval on-up on-down]} options]
    (.stopPropagation evt)
    (case (.-key evt)
      ("Control" "Alt" "Meta" "ContextMenu")
      (swap! complete-atom assoc :show-all true)

      "Tab"
      (swap! complete-atom cycle-completions (.-shiftKey evt) inst evt)

      "Enter"
      (when (should-eval? inst evt)
        (.preventDefault evt)
        (on-eval (.. inst -state -doc toString)))

      "ArrowUp"
      (when (and (not (.-shiftKey evt))
                 (utils.codemirror/first-line? inst))
        (.preventDefault evt)
        (on-up))

      "ArrowDown"
      (when (and (not (.-shiftKey evt))
                 (utils.codemirror/last-line? inst))
        (.preventDefault evt)
        (on-down))

      nil)))
