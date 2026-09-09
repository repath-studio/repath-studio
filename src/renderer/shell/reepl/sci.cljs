(ns renderer.shell.reepl.sci
  "Evaluates shell forms with [sci](https://github.com/babashka/sci), a small
   ClojureScript interpreter, against the app's own namespaces.

   Completions and docs come from a build-time generated data file (see
   build.shell-dsl-generator) and fall back to the compiler-inlined `user`
   namespace metadata when that file is unavailable."
  (:require
   [clojure.edn]
   [clojure.string :as string]
   [sci.core :as sci]
   [user])
  (:import goog.net.XhrIo))

(def dsl-url "js/shell-dsl.edn")

(defonce ctx (atom nil))
(defonce current-ns-ref (atom 'user))
(defonce dsl-data (atom nil))

(defn fetch-file!
  "Very simple implementation of XMLHttpRequests that given a file path
   calls src-cb with the string fetched or nil in case of error.
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

(defn- make-ctx
  []
  (sci/init
   {:classes {'js js/globalThis
              :allow :all}
    :namespaces {'user (into {} (map (fn [[n v]] [n (deref v)])
                                     (ns-publics 'user)))}
    ;; sci has no built-in CLJS data readers, so support the #js literal by
    ;; converting the read form to a JS value (keyword keys are preserved,
    ;; mirroring the CLJS reader).
    :readers (fn [tag]
               (when (= tag 'js)
                 clj->js))}))

(defn context
  []
  (if (nil? @ctx)
    (reset! ctx (make-ctx))
    @ctx))

(defn current-ns
  []
  (str @current-ns-ref))

(defn init!
  [cb]
  (try
    (let [done #(cb nil)]
      (context)
      (if (nil? @dsl-data)
        (fetch-file! dsl-url
                     (fn [text]
                       (when text
                         (try
                           (reset! dsl-data (clojure.edn/read-string text))
                           (catch :default err
                             (js/console.warn
                              "Failed to load the shell DSL data." err))))
                       (done)))
        (done)))
    (catch :default e
      (cb (cljs.core/Throwable->map e)))))

(defn- eval-forms-verbose
  [sci-ctx ns-sym text]
  (let [reader (sci/source-reader text)]
    (loop [results []
           ns-sym* ns-sym]
      (let [[form source] (sci/parse-next+string sci-ctx reader {:eof ::eof})]
        (if (= ::eof form)
          (do
            (doseq [v (butlast results)]
              (println (pr-str v)))
            [(when (seq results) (last results))
             ns-sym*])
          (let [result (sci/eval-string+ sci-ctx source
                                         {:ns (sci/create-ns ns-sym*)})]
            (recur (conj results (:val result))
                   (sci/ns-name (:ns result)))))))))

(defn execute
  [text verbose cb]
  (let [text (.trim (str text))]
    (if-not (seq text)
      (cb :output nil)
      (try
        (let [sci-ctx (context)
              ns-obj (sci/create-ns @current-ns-ref)
              [last-val final-ns] (if verbose
                                    (eval-forms-verbose sci-ctx
                                                        @current-ns-ref text)
                                    (let [result
                                          (sci/eval-string+ sci-ctx text
                                                            {:ns ns-obj})]
                                      [(:val result)
                                       (sci/ns-name (:ns result))]))]
          (reset! current-ns-ref final-ns)
          (cb :output last-val))
        (catch :default e
          ;; The value :via might not be serializable to JSON.
          (cb :error (dissoc (cljs.core/Throwable->map e) :via)))))))

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
  "Sorting algo for namespaces.

   The current ns comes first, then cljs.core, then anything else
   alphabetically"
  [current ns1 ns2]
  (cond
    (= ns1 current) -1
    (= ns2 current) 1
    (= ns1 "cljs.core") -1
    (= ns2 "cljs.core") 1
    :else (compare ns1 ns2)))

(defn js-attrs
  [obj]
  (if-not obj
    []
    (let [proto (js/Object.getPrototypeOf obj)]
      (concat (js/Object.getOwnPropertyNames obj)
              (when (and proto
                         (not= proto (.-prototype js/Object))
                         (not= proto obj))
                (js-attrs proto))))))

(def exclusions
  ["module$"
   "clojure$"
   "cljs$"
   "at_keyframes_styles_name$"
   "as__QMARK_qname_"
   "map_like_QMARK__"
   "rewrite_clj$"
   "sci$"
   "factory_name"
   "constructor"
   "fipp$"
   "shadow$"
   "day8$"
   "devtools$"
   "re_frame$"
   "reagent$"
   "camel_snake_kebab$"
   "malli$"
   "taoensso$"
   "get_default_error_fn_"
   "clj_"
   "_"
   "g_"
   "hickory$"
   "temp__"])

(defn- excluded?
  [name*]
  (or (string/includes? name* "_name$_")
      (some #(string/starts-with? name* %)
            exclusions)))

(defonce global-names (atom nil))

(defn refresh-global-names!
  []
  (reset! global-names
          (->> (js-attrs js/window)
               (remove excluded?)
               (distinct)
               (vec))))

(defn js-completion
  [text prefix]
  (let [parts (vec (.split text "."))
        head (or (last parts) "")
        names (if (seq (butlast parts))
                (when-let [obj (reduce (fn [acc k]
                                         (when-not (nil? acc)
                                           (aget acc k)))
                                       js/window
                                       (butlast parts))]
                  (->> (js-attrs obj)
                       (remove excluded?)
                       (distinct)))
                (or @global-names (refresh-global-names!)))]
    (when (seq names)
      (->> names
           (filter #(not= -1 (.indexOf % head)))
           (sort (partial compare-completion head))
           (map (fn [name*]
                  (vector nil
                          (str prefix
                               (string/join "." (conj (vec (butlast parts))
                                                      name*))))))))))

(defn doc-from-sym
  [sym]
  (let [ns* (or (namespace sym) (str @current-ns-ref))
        name* (name sym)
        ns* (or (get-in @dsl-data [:aliases (str @current-ns-ref) ns*]) ns*)
        data (get-in @dsl-data [:namespaces ns* name*])
        ;; Fallback: var metadata of the compiler-inlined `user`
        ;; namespace.
        var-meta (when (= ns* (str @current-ns-ref))
                   (when-let [v (get (ns-publics 'user) (symbol name*))]
                     (meta v)))]
    (cond
      (or (:doc data) (:arglists data))
      {:name (str ns* "/" name*)
       :type :normal
       :forms (:arglists data)
       :doc (:doc data)}

      var-meta
      {:name (str ns* "/" name*)
       :type :normal
       :forms (:arglists var-meta)
       :doc (:doc var-meta)}

      :else
      nil)))

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

(defn print-language-doc
  [doc render-arglist]
  (println (:name doc))
  (println)
  (when-let [arglists (seq (:forms doc))]
    (println (string/join " " (map render-arglist arglists))))
  (println)
  (when (:doc doc)
    (println (:doc doc))))
