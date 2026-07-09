(ns build.schema-graph-generator
  "Shadow-cljs build hook that watches src/renderer/**/db.cljs files and
   generates a DOT graph of the app's Malli schemas."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.walk :as walk]
   [malli.core :as m]
   [malli.dot :as md])
  (:import
   [java.io File]
   [java.nio.file FileSystems StandardWatchEventKinds WatchService]))

(defn renderer-src-dir [] (io/file "src" "renderer"))
(defn dot-output-file [] (io/file "schema-explorer/graph.dot"))

(def ns-resolver
  "LispReader$Resolver that returns namespace aliases unchanged so that
   ::alias/kw is read as :alias/kw rather than throwing."
  (reify clojure.lang.LispReader$Resolver
    (currentNS [_] (symbol "build.schema-graph-generator"))
    (resolveClass [_ _sym] nil)
    (resolveAlias [_ sym] sym)
    (resolveVar [_ _sym] nil)))

(defn read-all-forms
  "Read all top-level ClojureScript forms from file.
   Stops on the first unrecoverable reader error."
  [^File file]
  (try
    (with-open [rdr (java.io.PushbackReader. (io/reader file))]
      (binding [*reader-resolver* ns-resolver]
        (let [eof (Object.)]
          (loop [acc []]
            (let [form (try (read {:eof eof} rdr)
                            (catch Exception _ eof))]
              (if (identical? form eof)
                acc
                (recur (conj acc form))))))))
    (catch Exception e
      (println "[schema-graph-generator] Could not read"
               (.getName file) "-" (.getMessage e))
      [])))

(def malli-tags
  "https://github.com/metosin/malli#built-in-schemas"
  #{:> :>= :< :<= := :not= :any :some :nil :string :int :double
    :boolean :keyword :qualified-keyword :symbol :qualified-symbol :uuid
    :+ :* :? :repeat :cat :alt :catn :altn :and :or :orn :not :map
    :map-of :vector :sequential :set :enum :maybe :tuple :multi :re
    :fn :ref :=> :-> :function :schema :merge :union :select-keys})

(defn schema-form?
  "True if form looks like a malli schema (tagged vector or predicate symbol)."
  [form]
  (or (and (vector? form)
           (keyword? (first form))
           (contains? malli-tags (first form)))
      (and (symbol? form)
           (string/ends-with? (name form) "?"))))

(defn extract-schema-defs
  "Return [{:label str :form form}] for every schema def found in forms.
   Handles optional docstrings: (def Name \"doc\" schema)."
  [forms]
  (for [form forms
        :when (and (seq? form)
                   (= 'def (first form))
                   (>= (count form) 3))
        :let [sym (second form)
              ;; Skip docstring if present
              value (let [v-rest (drop 2 form)]
                      (if (and (string? (first v-rest)) (next v-rest))
                        (second v-rest)
                        (first v-rest)))]
        :when (schema-form? value)]
    {:label (name sym)
     :form value}))

(defn sanitize-for-clj
  "Postwalk a schema form to make it safe for Clojure-side malli:
   - [:fn ...]        -> :any  (strips ClojureScript predicates)
   - js/Foo           -> :any  (strips JS interop symbols)
   - other-ns/sym     -> nil   (only appears in :default values; safe)
   - KnownType symbol -> [:ref `KnownType`]"
  [form known-types]
  (walk/postwalk
   (fn [x]
     (cond
       (and (vector? x) (= :fn (first x)))
       :any

       (and (symbol? x) (= "js" (namespace x)))
       :any

       (and (symbol? x) (some? (namespace x)))
       nil

       (and (symbol? x)
            (nil? (namespace x))
            (contains? known-types (name x)))
       [:ref (name x)]

       :else x))
   form))

(defn find-db-files
  [^File dir]
  (->> (file-seq dir)
       (filter #(= "db.cljs" (.getName ^File %)))))

(defn resolve-simple-values
  "Replace unqualified symbols that map to known literal values (numbers,
   strings, booleans). Used to inline file-local constants like min-zoom."
  [form value-map]
  (if (empty? value-map)
    form
    (walk/postwalk
     (fn [x]
       (if (and (symbol? x) (nil? (namespace x)) (contains? value-map (name x)))
         (get value-map (name x))
         x))
     form)))

(defn add-fallback-refs
  [registry]
  (let [referenced (atom #{})]
    (doseq [[_ schema] registry]
      (walk/postwalk
       (fn [x]
         (when (and (vector? x)
                    (= :ref (first x))
                    (string? (second x)))
           (swap! referenced conj (second x)))
         x)
       schema))
    (let [missing (remove #(contains? registry %) @referenced)]
      (into registry (map (fn [t] [t :any]) missing)))))

(defn build-registry
  []
  (let [per-file (->> (find-db-files (renderer-src-dir))
                      (map (fn [^File f]
                             (let [forms (read-all-forms f)
                                   defs (filter #(and (seq? %)
                                                      (= 'def (first %))
                                                      (>= (count %) 3))
                                                forms)
                                   ;; Get the actual value from a def, skipping
                                   ;; an optional docstring as second element.
                                   def-val (fn [d]
                                             (let [tail (drop 2 d)]
                                               (if (and (string? (first tail))
                                                        (next tail))
                                                 (second tail)
                                                 (first tail))))]
                               {:schema-defs (extract-schema-defs forms)
                                ;; Simple literal value defs for local inlining
                                ;; (e.g. min-zoom 0.01, max-zoom 100)
                                :local-values
                                (->> defs
                                     (keep (fn [d]
                                             (let [v (def-val d)]
                                               (when (or (number? v)
                                                         (string? v)
                                                         (boolean? v))
                                                 [(name (second d)) v]))))
                                     (into {}))
                                ;; All PascalCase def names contribute to
                                ;; known-types, even those defined via calls
                                ;; (e.g. RecentDocument, PersistedDocument)
                                :type-names
                                (->> defs
                                     (map #(name (second %)))
                                     (filter #(and (seq %)
                                                   (Character/isUpperCase
                                                    (first ^String %)))))})))
                      vec)
        known-types (into #{} (mapcat :type-names per-file))
        registry (->> per-file
                      (mapcat (fn [{:keys [schema-defs local-values]}]
                                (map (fn [{:keys [label form]}]
                                       [label (-> form
                                                  (resolve-simple-values
                                                   local-values)
                                                  (sanitize-for-clj
                                                   known-types))])
                                     schema-defs)))
                      (into {}))]
    (add-fallback-refs registry)))

(defn generate-dot-string
  [registry]
  (try
    (-> [:schema {:registry registry} "App"]
        m/schema
        md/transform)
    (catch Exception e
      (println "schema-graph-generator] DOT generation failed:" (.getMessage e))
      nil)))

(defn generate!
  []
  (let [registry (build-registry)
        dot-str (generate-dot-string registry)]
    (if dot-str
      (let [dot-file (dot-output-file)]
        (io/make-parents dot-file)
        (spit dot-file dot-str)
        (println (str "[schema-graph-generator] Generated " (.getPath dot-file)
                      " (" (count registry) " schemas)")))
      (println "[schema-graph-generator] Skipped"
               " — DOT string could not be generated"))))

(defonce ^:private watcher-state (atom nil))

(defn stop-watcher! []
  (when-let [{:keys [^WatchService ws ^Thread thread]} @watcher-state]
    (println "[schema-graph-generator] Stopping schema watcher")
    (.close ws)
    (.interrupt thread)
    (reset! watcher-state nil)))

(defn db-file-event?
  [^java.nio.file.WatchEvent event]
  (= "db.cljs" (str (.context event))))

(defn start-watcher!
  []
  (stop-watcher!)
  (let [ws (.newWatchService (FileSystems/getDefault))
        kinds (into-array [StandardWatchEventKinds/ENTRY_CREATE
                           StandardWatchEventKinds/ENTRY_MODIFY
                           StandardWatchEventKinds/ENTRY_DELETE])]
    (doseq [^File d (->> (file-seq (renderer-src-dir))
                         (filter #(.isDirectory ^File %)))]
      (-> d .toPath (.register ws kinds)))
    (let [thread (Thread.
                  (fn []
                    (println "[schema-graph-generator] Watching db.cljs files"
                             " under src/renderer/")
                    (try
                      (loop []
                        (when-let [wk (.take ws)]
                          (let [events (.pollEvents wk)
                                changed? (some db-file-event? events)]
                            (.reset wk)
                            (when changed?
                              (Thread/sleep 100)
                              (generate!))
                            (recur))))
                      (catch java.nio.file.ClosedWatchServiceException _)
                      (catch InterruptedException _))))]
      (doto thread
        (.setName "schema-graph-generator-watcher")
        (.setDaemon true)
        (.start))
      (reset! watcher-state {:ws ws
                             :thread thread}))))

(defn ^:export hook!
  {:shadow.build/stages #{:configure :compile-prepare}}
  [build-state & _args]
  (case (:shadow.build/stage build-state)
    :configure (do (start-watcher!) (generate!) build-state)
    :compile-prepare (do (generate!) build-state)))
