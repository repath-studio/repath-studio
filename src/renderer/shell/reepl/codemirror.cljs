(ns renderer.shell.reepl.codemirror
  (:require
   [clojure.edn :as edn]
   [clojure.string :as string]))

;; TODO: can we avoid the global state modification here?
#_(js/CodeMirror.registerHelper
   "wordChars"
   "clojure"
   #"[^\s\(\)\[\]\{\},`']")

(def wordChars
  "[^\\s\\(\\)\\[\\]\\{\\},`']*")

(defn word-in-line
  [line lno cno]
  (let [back (-> line
                 (.slice 0 cno)
                 (.match (js/RegExp. (str wordChars "$")))
                 (first))
        forward (-> line
                    (.slice cno)
                    (.match (js/RegExp. (str "^" wordChars)))
                    (first))]
    {:start #js {:line lno
                 :ch (- cno (count back))}
     :end #js {:line lno
               :ch (+ cno (count forward))}}))

(defn valid-cljs?
  [source]
  (try (boolean (edn/read-string source))
       (catch js/Error _err false)))

(defn should-go-up?
  [_source inst]
  (let [pos (.getCursor inst)]
    (= 0 (.-line pos))))

(defn should-go-down?
  [_source inst]
  (let [pos (.getCursor inst)
        last-line (.lastLine inst)]
    (= last-line (.-line pos))))

(defn in-place?
  [inst]
  (let [lines (.lineCount inst)]
    (or (= 1 lines)
        (let [pos (.getCursor inst)
              last-line (dec lines)]
          (and
           (= last-line (.-line pos))
           (= (.-ch pos)
              (count (.getLine inst last-line))))))))

(defn should-eval?
  [source inst evt]
  (cond
    (.-shiftKey evt) false
    (.-metaKey evt) true
    :else (and (in-place? inst)
               (valid-cljs? source))))

(defn cm-current-word
  "Find the current 'word' according to CodeMirror's `wordChars' list"
  [cm]
  (let [pos (.getCursor cm)
        lno (.-line pos)
        cno (.-ch pos)
        line (.getLine cm lno)]
    ;; findWordAt doesn't work w/ clojure-parinfer mode
    ;; (.findWordAt cm back)
    (word-in-line line lno cno)))

(defn repl-hint
  "Get a new completion state."
  [complete-word cm _options]
  (let [result (cm-current-word cm)
        text (.getRange cm
                        (:start result)
                        (:end result))
        words (when-not (empty? text)
                (vec (complete-word text)))
        ;; Remove core duplicates
        words (vec (remove #(string/includes? (second %) "cljs.core") words))]
    (when-not (empty? words)
      {:words words
       :num (count words)
       :active (= (get (first words) 2) text)
       :show-all false
       :initial-text text
       :pos 0
       :from (:start result)
       :to (:end result)})))

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
  (when (and state (or (< 1 (count words))
                       (and (< 0 (count words))
                            (not= initial-text (get (first words) 2)))))
    (.preventDefault evt)
    (let [initial-active (= initial-text (get (first words) 2))
          [active pos] (if active
                         (cycle-pos num pos go-back? initial-active)
                         [true (if go-back? (dec num) pos)])
          text (if active
                 (get (get words pos) 2)
                 initial-text)]
      ;; TODO: don't replaceRange here, instead watch the state atom and react
      ;; to that.
      (.replaceRange cm text from to)
      (assoc state
             :pos pos
             :active active
             :to #js {:line (.-line from)
                      :ch (+ (count text)
                             (.-ch from))}))))

(def cancel-keys #{13 27})
(def cmp-ignore #{9 16 17 18 91 93})
(def cmp-show #{17 18 91 93})

(defn on-keyup-handler
  [options inst evt]
  (let [{:keys [complete-atom complete-word]} options]
    (.stopPropagation evt)
    (if (cancel-keys (.-keyCode evt))
      (if @complete-atom
        (reset! complete-atom nil)
        (some-> (.-activeElement js/document)
                (.blur)))
      (if (cmp-show (.-keyCode evt))
        (swap! complete-atom assoc :show-all false)
        (when-not (cmp-ignore (.-keyCode evt))
          (reset! complete-atom (repl-hint complete-word inst nil)))))))

(defn on-keydown-handler
  [options inst evt]
  (let [{:keys [complete-atom on-eval on-up on-down]} options]
    (.stopPropagation evt)
    (case (.-keyCode evt)
      (17 18 91 93)
      (swap! complete-atom assoc :show-all true)
      ;; tab
      9 (swap! complete-atom
               cycle-completions
               (.-shiftKey evt)
               inst
               evt)
      ;; enter
      13 (let [source (.getValue inst)]
           (when (should-eval? source inst evt)
             (.preventDefault evt)
             (on-eval source)))
      ;; up
      38 (let [source (.getValue inst)]
           (when (and (not (.-shiftKey evt))
                      (should-go-up? source inst))
             (.preventDefault evt)
             (on-up)))
      ;; down
      40 (let [source (.getValue inst)]
           (when (and (not (.-shiftKey evt))
                      (should-go-down? source inst))
             (.preventDefault evt)
             (on-down)))

      :none)))



