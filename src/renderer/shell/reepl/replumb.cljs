(ns renderer.shell.reepl.replumb
  (:require
   #_[shadow.cljs.bootstrap.browser :as bootstrap]
   [cljs.js]
   [cljs.tagged-literals :as tags]
   [cljs.tools.reader]
   [cljs.tools.reader.reader-types :refer [string-push-back-reader]]
   [clojure.string :as string]
   [replumb.ast :as ast]
   [replumb.core :as replumb]
   [replumb.doc-maps :as docs]
   [replumb.repl :as repl])
  (:import goog.net.XhrIo))

(defn fetch-file!
  "Very simple implementation of XMLHttpRequests that given a file path
  calls src-cb with the string fetched of nil in case of error.
  See doc at https://developers.google.com/closure/library/docs/xhrio"
  [file-url src-cb]
  (try
    (.send XhrIo file-url
           (fn [e]
             (if (.isSuccess (.-target ^js e))
               (src-cb (.. ^js e -target getResponseText))
               (src-cb nil))))
    (catch :default _err
      (src-cb nil))))

(def replumb-opts
  (merge (replumb/options
          :browser
          ["js/bootstrap/src"]
          ;; TODO: figure out file loading
          #_(fn [& a] nil)
          fetch-file!)
         {:warning-as-error true
          :verbose false
          ;; :load-fn! (fn [z cb])
          :no-pr-str-on-value true}))
cljs.js/*load-fn*

#_(defn find-last-expr-pos [text]
    ;; parse #js {} correctly
    (binding [cljs.tools.reader/*data-readers* tags/*cljs-data-readers*]
      (let [rr (string-push-back-reader text)
            ;; get a unique js object as a sigil
            eof (js-obj)
            read #(cljs.tools.reader/read {:eof eof} rr)]
        (loop [last-pos 0 second-pos 0 last-form nil _second-form nil]
          (let [form (read)
                new-pos (.-s-pos (.-rdr ^js rr))]
            (if (identical? eof form)
              second-pos;; second-form]
              (recur new-pos last-pos form last-form)))))))

#_(defn make-last-expr-set-val [text js-name]
    (let [last-pos (find-last-expr-pos text)]
      ;; (js/console.log last-pos text)
      (when-not (= last-pos 0)
        (str
         (.slice text 0 last-pos)
         "(aset js/window \"" js-name "\" "
         (.slice text last-pos)
         ")"))))

#_(defn jsc-run [source cb]
    (cljs.js/eval-str repl/st
                      source
                      'stuff
                      {:eval cljs.js/js-eval
                       :ns (repl/current-ns)
                       :load (partial bootstrap/load repl/st)
                       :context :statement
                       :def-emits-var true}
                      (fn [result]
                        (swap! repl/app-env assoc :current-ns (:ns result))
                        (if (contains? result :error)
                          (cb false (:error result))
                          (cb true (aget js/window "last_repl_value"))))))

(defn get-first-form
  [text]
  ;; parse #js {} correctly
  (binding [cljs.tools.reader/*data-readers* tags/*cljs-data-readers*]
    (let [rr (string-push-back-reader text)
          form (cljs.tools.reader/read rr)
          ;; TODO: this is a bit dependent on tools.reader internals...
          s-pos (.-s-pos (.-rdr ^js rr))]
      [form s-pos])))

(defn run-repl-multi
  [text opts cb]
  (let [text (.trim text)
        [_form pos] (get-first-form text)
        source (.slice text 0 pos)
        remainder (.trim (.slice text pos))
        has-more? (seq remainder)]
    ;; (js/console.log [text form pos source remainder has-more?])
    (replumb/read-eval-call
     opts
     #(let [success? (replumb/success? %)
            result (replumb/unwrap-result %)]
        ;; (js/console.log "evaled" [success? result has-more?])
        (if-not success?
          (cb success? result)
          ;; TODO: should I log the result if it's not the end?
          (if has-more?
            (run-repl-multi remainder opts cb)
            (cb success? result))))
     source)))

;; Trying to get expressions + statements to play well together
;; TODO: is this a better way? The `do' stuff seems to work alright ... although
;; it won't work if there are other `ns' statements inside there...
#_(defn run-repl-experimental* \
 [text opts cb]
    (let [fixed (make-last-expr-set-val text "last_repl_value")]
      (if fixed
        (jsc-run fixed cb)
        (replumb/read-eval-call
         opts #(cb (replumb/success? %) (replumb/unwrap-result %)) text))))

#_(defn fix-ns-do
    [text]
    ;; parse #js {} correctly
    (binding [cljs.tools.reader/*data-readers* tags/*cljs-data-readers*]
      (let [rr (string-push-back-reader text)
            form (cljs.tools.reader/read rr)
            is-ns (and (sequential? form)
                       (= 'ns (first form)))
            ;; TODO: this is a bit dependent on tools.reader internals...
            s-pos (.-s-pos (.-rdr ^js rr))]
        ;; (js/console.log is-ns form s-pos)
        (if-not is-ns
          (str "(do " text ")")
          (str
           (.slice text 0 s-pos)
           "(do "
           (.slice text s-pos)
           ")")))))

#_(defn run-repl*
    [text opts cb]
    (replumb/read-eval-call
     opts
     #(cb
       (replumb/success? %)
       (replumb/unwrap-result %))
     (fix-ns-do text)))

(defn run-repl
  ([text cb] (run-repl-multi text replumb-opts cb))
  ([text opts cb] (run-repl-multi text (merge replumb-opts opts) cb)))

(defn compare-completion
  "The comparison algo for completions

  1. if one is exactly the text, then it goes first
  2. if one *starts* with the text, then it goes first
  3. otherwise leave in current order"
  [text a b]
  (cond
    (and (= text a)
         (= text b)) 0
    (= text a) -1
    (= text b) 1
    :else
    (let [a-starts (zero? (.indexOf a text))
          b-starts (zero? (.indexOf b text))]
      (cond
        (and a-starts b-starts) 0
        a-starts -1
        b-starts 1
        :else 0))))

(defn compare-ns
  "Sorting algo for namespaces

  The current ns comes first, then cljs.core, then anything else
  alphabetically"
  [current ns1 ns2]
  (cond
    (= ns1 current) -1
    (= ns2 current) 1
    (= ns1 'cljs.core) -1
    (= ns2 'cljs.core) 1
    :else (compare ns1 ns2)))

(defn get-from-js-ns
  "Use js introspection to get a list of interns in a namespace.

  This is pretty dependent on cljs runtime internals, so it may break in the
  future (although I think it's fairly unlikely). It takes advantage of the fact
  that the ns `something.other.thing' is available as an object on
  `window.something.other.thing', and Object.keys gets all the variables in that
  namespace."
  [ns*]

  (let [parts (map munge (.split (str ns*) "."))
        ns* (reduce aget js/window parts)]
    (if-not ns*
      []
      (map demunge (js/Object.keys ns*)))))

(defn dedup-requires
  "Takes a map of {require-name ns-name} and dedups multiple keys that have the
  same ns-name value."
  [requires]
  (first
   (reduce (fn [[result seen] [k v]]
             (if (seen v)
               [result seen]
               [(assoc result k v) (conj seen v)])) [{} #{}] requires)))

(defn get-matching-ns-interns [[name* ns*] matches? only-ns]
  (let [ns-name* (str ns*)
        publics (keys (ast/ns-publics @repl/st ns*))
        publics (if (empty? publics)
                  (get-from-js-ns ns*)
                  publics)]
    (if-not (or (nil? only-ns)
                (= only-ns ns-name*))
      []
      (sort (map #(symbol name* (str %))
                 (filter matches?
                         publics))))))

(defn js-attrs [obj]
  (if-not obj
    []
    (let [_constructor (.-constructor obj)
          proto (js/Object.getPrototypeOf obj)]
      (concat (js/Object.keys obj)
              (when-not (= proto obj)
                (js-attrs proto))))))

(defn js-completion
  [mode text]
  (let [parts (vec (.split text "."))
        completion (or (last parts) "")
        possibles (js-attrs (reduce aget js/window (butlast parts)))
        prefix #(->> (conj (vec (butlast parts)) %)
                     (string/join ".")
                     (str (when (= mode :cljs) "js/")))]
    (->> possibles
         (filter #(not= -1 (.indexOf % completion)))
         (sort (partial compare-completion text))
         (map #(vector nil (prefix %) (prefix %))))))

;; TODO: fuzzy-match if there are no normal matches
(defn cljs-completion
  "Tab completion, copied w/ extensive modifications from
   replumb.repl/process-apropos."
  [text]
  (let [[only-ns text] (if-not (= -1 (.indexOf text "/"))
                         (.split text "/")
                         [nil text])
        matches? #(and
                   ;; TODO: find out what these t_cljs$core things are...
                   ;; They seem to be nil.
                   (= -1 (.indexOf (str %) "t_cljs$core"))
                   (< -1 (.indexOf (str %) text)))
        current-ns (repl/current-ns)
        replace-name (fn [sym]
                       (if (or (= (namespace sym) "cljs.core")
                               (= (namespace sym) (str current-ns)))
                         (name sym)
                         (str sym)))
        requires (:requires
                  (ast/namespace @repl/st current-ns))
        only-ns (when only-ns
                  (or (str (get requires (symbol only-ns)))
                      only-ns))
        requires (concat
                  [[nil current-ns]
                   [nil 'cljs.core]]
                  (dedup-requires (vec requires)))
        names (set (apply concat requires))
        defs (->> requires
                  (sort-by second (partial compare-ns current-ns))
                  (mapcat #(get-matching-ns-interns % matches? only-ns))
                  ;; [qualified symbol, show text, replace text]
                  (map #(vector % (str %) (replace-name %) (name %)))
                  (sort-by #(get % 3) (partial compare-completion text)))]
    (vec (concat
          ;; TODO: make this configurable
          (take 75 defs)
          (map
           #(vector % (str %) (str %))
           (filter matches? names))))))

(defn process-apropos
  [mode text]
  (case mode
    :js (js-completion mode text)
    :python (js-completion mode text)
    :cljs (if (zero? (.indexOf text "js/"))
            (js-completion mode (.slice text 3))
            (cljs-completion text))))

(defn get-forms
  [m]
  (cond
    (:forms m) (:forms m)
    (:arglists m) (let [arglists (:arglists m)]
                    (if (or (:macro m)
                            (:repl-special-function m))
                      arglists
                      (if (= 'quote (first arglists))
                        (second arglists)
                        arglists)))))

;; Copied & modified from cljs.repl/print-doc
(defn get-doc
  [m]
  (merge {:name (str (when (:ns m) (str (:ns m) "/")) (:name m))
          :type (cond
                  (:protocol m) :protocol
                  (:special-form m) :special-form
                  (:macro m) :macro
                  (:repl-special-function m) :repl-special-function
                  :else :normal)
          :forms (get-forms m)
          :doc (:doc m)}
         (if (:special-form m)
           {:please-see (if (contains? m :url)
                          (when (:url m)
                            (str "http://clojure.org/" (:url m)))
                          (str "http://clojure.org/special_forms#" (:name m)))}
           (when (:protocol m)
             {:protocol-methods (:methods m)}))))

(defn doc-from-sym
  [sym]
  (cond
    (docs/special-doc-map sym) (get-doc (docs/special-doc sym))
    (docs/repl-special-doc-map sym) (get-doc (docs/repl-special-doc sym))
    (ast/namespace
     @repl/st sym) (get-doc
                    (select-keys
                     (ast/namespace @repl/st sym)
                     [:name :doc]))
    :else (get-doc
           (repl/get-var
            nil
            sym))))

(def type-name
  {:protocol "Protocol"
   :special-form "Special Form"
   :macro "Macro"
   :repl-special-function "REPL Special Function"})

;; Copied & modified from cljs.repl/print-doc
(defn print-doc
  [doc]
  (println (:name doc))
  (println)
  (when-not (= :normal (:type doc))
    (println (type-name (:type doc))))
  (when (:forms doc)
    (prn (:forms doc)))
  (when (:please-see doc)
    (println (str "\n  Please see " (:please-see doc))))
  (when (:doc doc)
    (println)
    (println (:doc doc)))
  (when (:methods doc)
    (doseq [[name* {:keys [doc arglists]}] (:methods doc)]
      (println)
      (println " " name*)
      (println " " arglists)
      (when doc
        (println " " doc)))))

(defn process-doc
  "Get the documentation for a symbol. Copied & modified from replumb."
  [sym]
  (when sym
    (with-out-str
      (print-doc (doc-from-sym sym)))))
