(ns renderer.shell.reepl.codemirror
  (:require
   [clojure.string :as string]))

(defn should-go-up?
  [_source ^js inst]
  (let [pos (.. inst -state -selection -main -head)
        line (.lineAt (.. inst -state -doc) pos)]
    (zero? (.-from line))))

(defn should-go-down?
  [_source ^js inst]
  (let [pos (.. inst -state -selection -main -head)
        line (.lineAt (.. inst -state -doc) pos)
        last-line (.. inst -state -doc -lines)]
    (= (dec last-line) (.-from line))))

(defn in-place?
  [^js inst]
  (let [lines (.. inst -state -doc -lines)
        pos (.. inst -state -selection -main -head)
        line (.lineAt (.. inst -state -doc) pos)]
    (or (= 1 lines)
        (and (= lines (.-number line))
             (= pos (.-to line))))))

(defn should-eval?
  [inst evt]
  (cond
    (.-shiftKey evt) false
    (.-metaKey evt) true
    :else (in-place? inst)))

(defn cm-current-word
  "Find the current 'word' according to CodeMirror's `wordChars' list"
  [^js cm]
  (let [pos (.. cm -state -selection -main -head)]
    (or (.wordAt (.-state cm) pos)
        (let [char-before (.sliceDoc (.-state cm) (dec pos) pos)]
          (when-not (= char-before "/")
            char-before)))))

(defn repl-hint
  "Get a new completion state."
  [complete-word ^js cm _options]
  (when-let [result (cm-current-word cm)]
    (let [from (.-from result)
          to (.-to result)
          text (.sliceDoc (.-state cm) from to)
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
  "Cycle through positions. Returns [active new-pos].

  count
    total number of completions
  current
    current position
  go-back?
    should we be going in reverse
  initial-active
    if false, then we return not-active when wrapping around"
  [n current go-back initial-active]
  (if go-back
    (if (>= 0 current)
      (if initial-active
        [true (dec n)]
        [false 0])
      [true (dec current)])
    (if (>= current (dec n))
      [initial-active 0]
      [true (inc current)])))

(defn should-cycle?
  [{:keys [words initial-text]
    :as state}]
  (and state
       (or (< 1 (count words))
           (and (< 0 (count words))
                (not= initial-text (get (first words) 2))))))

(defn cycle-completions
  "Cycle through completions, changing the codemirror text accordingly. Returns
  a new state map.

  state
    the current completion state
  go-back?
    whether to cycle in reverse (generally b/c shift is pressed)
  cm
    the codemirror instance
  evt
    the triggering event. it will be `.preventDefault'd if there are completions
    to cycle through."
  [{:keys [num pos active from to words initial-text]
    :as state}
   go-back? cm evt]
  (when (should-cycle? state)
    (.preventDefault evt)
    (let [initial-active (= initial-text (get (first words) 2))
          [active pos] (if active
                         (cycle-pos num pos go-back? initial-active)
                         [true (if go-back? (dec num) pos)])
          text (if active
                 (get (get words pos) 2)
                 initial-text)]
      (.dispatch cm #js {:changes #js {:from from
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
      (let [source (.. inst -state -doc toString)]
        (when (should-eval? inst evt)
          (.preventDefault evt)
          (on-eval source)))

      "ArrowUp"
      (let [source (.. inst -state -doc toString)]
        (when (and (not (.-shiftKey evt))
                   (should-go-up? source inst))
          (.preventDefault evt)
          (on-up)))

      "ArrowDown"
      (let [source (.. inst -state -doc toString)]
        (when (and (not (.-shiftKey evt))
                   (should-go-down? source inst))
          (.preventDefault evt)
          (on-down)))

      nil)))
