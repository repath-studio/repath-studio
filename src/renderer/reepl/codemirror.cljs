(ns renderer.reepl.codemirror
  (:require
   ["codemirror" :as codemirror]
   ["codemirror/addon/edit/closebrackets.js"]
   ["codemirror/addon/edit/matchbrackets.js"]
   ["codemirror/addon/hint/show-hint.js"]
   ["codemirror/addon/runmode/colorize.js"]
   ["codemirror/addon/runmode/runmode.js"]
   ["codemirror/mode/clojure/clojure.js"]
   ["codemirror/mode/javascript/javascript.js"]
   ["react" :as react]
   [clojure.edn :as edn]
   [clojure.string :as string]
   [reagent.core :as reagent]))

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

(def default-opts
  {:style {:height "auto"
           :flex 1}

   :should-go-up
   (fn [_source inst]
     (let [pos (.getCursor inst)]
       (= 0 (.-line pos))))

   :should-go-down
   (fn [_source inst]
     (let [pos (.getCursor inst)
           last-line (.lastLine inst)]
       (= last-line (.-line pos))))

   ;; TODO: if the cursor is inside a list, and the function doesn't have enough
   ;; arguments yet, then return false
   ;; e.g. (map |) <- map needs at least one argument.
   :should-eval
   (fn [source inst evt]
     (if (.-shiftKey evt)
       false
       (if (.-metaKey evt)
         true
         (let [lines (.lineCount inst)
               in-place (or (= 1 lines)
                            (let [pos (.getCursor inst)
                                  last-line (dec lines)]
                              (and
                               (= last-line (.-line pos))
                               (= (.-ch pos)
                                  (count (.getLine inst last-line))))))]
           (and in-place
                (valid-cljs? source))))))})

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
      (if initial-active
        [true 0]
        [false 0])
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

;; TODO: refactor this to be smaller
(defn code-mirror
  "Create a code-mirror editor that knows a fair amount about being a good
  repl. The parameters:

  value-atom (reagent atom)
    when this changes, the editor will update to reflect it.

  options (TODO: finish documenting)

  :style (reagent style map)
    will be applied to the container element

  :on-change (fn [text] -> nil)
  :on-eval (fn [text] -> nil)
  :on-up (fn [] -> nil)
  :on-down (fn [] -> nil)
  :should-go-up
  :should-go-down
  :should-eval

  :js-cm-opts
    options passed into the CodeMirror constructor

  :on-cm-init (fn [cm] -> nil)
    called with the CodeMirror instance, for whatever extra fiddling you want to
    do."
  [value-atom options]
  (let [cm (atom nil)
        ref (react/createRef)
        options (merge default-opts options)
        {:keys [style
                on-change
                on-eval
                on-up
                on-down
                complete-atom
                complete-word
                should-go-up
                should-go-down
                should-eval
                js-cm-opts
                on-cm-init]} options]
    (reagent/create-class
     {:component-did-mount
      (fn [_this]
        (let [dom-el (.-current ref)
              ;; On Escape, should we revert to the pre-completion-text?
              cancel-keys #{13 27}
              cmp-ignore #{9 16 17 18 91 93}
              cmp-show #{17 18 91 93}
              inst (codemirror
                    dom-el
                    (clj->js
                     (merge
                      {:lineNumbers false
                       :viewportMargin js/Infinity
                       :matchBrackets true
                       :lineWrapping true
                       :theme "tomorrow-night-eighties"
                       :autofocus false
                       :extraKeys #js {"Shift-Enter" "newlineAndIndent"}
                       :value @value-atom
                       :autoCloseBrackets true
                       :mode "clojure"
                       :screenReaderLabel "REPL"}
                      js-cm-opts)))]

          (reset! cm inst)
          (.on inst "change"
               (fn []
                 (let [value (.getValue inst)]
                   (when-not (= value @value-atom)
                     (on-change value)))))

          (.on inst "keyup"
               (fn [inst evt]
                 (.stopPropagation evt)
                 (if (cancel-keys (.-keyCode evt))
                   (reset! complete-atom nil)
                   (if (cmp-show (.-keyCode evt))
                     (swap! complete-atom assoc :show-all false)
                     (when-not (cmp-ignore (.-keyCode evt))
                       (reset! complete-atom
                               (repl-hint complete-word inst nil)))))))

          (.on inst "keydown"
               (fn [inst evt]
                 (.stopPropagation evt)
                 (case (.-keyCode evt)
                   (17 18 91 93)
                   (swap! complete-atom assoc :show-all true)
                   ;; tab
                   ;; TODO: do I ever want to use TAB normally?
                   ;; Maybe if there are no completions...
                   ;; Then I'd move this into cycle-completions?
                   9 (swap! complete-atom
                            cycle-completions
                            (.-shiftKey evt)
                            inst
                            evt)
                   ;; enter
                   13 (let [source (.getValue inst)]
                        (when (should-eval source inst evt)
                          (.preventDefault evt)
                          (on-eval source)))
                   ;; up
                   38 (let [source (.getValue inst)]
                        (when (and (not (.-shiftKey evt))
                                   (should-go-up source inst))
                          (.preventDefault evt)
                          (on-up)))
                   ;; down
                   40 (let [source (.getValue inst)]
                        (when (and (not (.-shiftKey evt))
                                   (should-go-down source inst))
                          (.preventDefault evt)
                          (on-down)))
                   :none)))
          (when on-cm-init
            (on-cm-init inst))))

      :component-did-update
      (fn [_this _old-argv]
        (when-not (= @value-atom (.getValue @cm))
          (.setValue @cm @value-atom)
          (let [last-line (.lastLine @cm)
                last-ch (count (.getLine @cm last-line))]
            (.setCursor @cm last-line last-ch))))

      :reagent-render
      (fn [_ _ _]
        @value-atom
        [:div {:ref ref
               :style style}])})))

(defn colored-text
  [_text _theme]
  (let [ref (react/createRef)]
    (reagent/create-class
     {:component-did-mount
      (fn [_this]
        (let [dom-el (.-current ref)]
          ((aget codemirror "colorize") #js[dom-el] "clojure")
          ;; Hacky way to remove the theme class added by CodeMirror's colorize
          ;; https://codemirror.net/addon/runmode/colorize.js
          (-> dom-el .-classList (.remove "cm-s-default"))))

      :reagent-render
      (fn [text theme]
        [:pre.p-0.m-0
         {:class (str "cm-s-" theme)
          :ref ref}
         text])})))
