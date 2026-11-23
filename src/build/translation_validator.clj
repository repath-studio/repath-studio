(ns build.translation-validator
  "Validates translation files under src/lang against the en-US base file.

   Checks that every language file defines exactly the same set of keys as
   en-US.edn, and that every base key exists as a `::key` in the ClojureScript
   namespace file it is declared under.

   Pass `--report` to print a per-language work list of missing keys, with
   their English source values and insertion anchors, instead of validating."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as string])
  (:import
   [java.io File]
   [java.util.regex Pattern]))

(def base-lang "en-US")

(defn lang-dir [] (io/file "src" "lang"))
(defn src-dir [] (io/file "src"))

(defn lang-files
  [dir]
  (->> (.listFiles ^File dir)
       (filter #(string/ends-with? (.getName ^File %) ".edn"))
       (sort-by #(.getName ^File %))))

(defn lang-id
  [^File f]
  (string/replace (.getName f) #"\.edn$" ""))

(defn read-lang
  [^File f]
  (edn/read-string (slurp f)))

(defn dictionary->keys
  "Flattens a language dictionary into a set of qualified keywords, e.g.
   {:renderer.i18n.core {:language \"Language\"}} becomes
   #{:renderer.i18n.core/language}. Top-level entries that aren't namespace
   maps (e.g. :missing) are kept as-is."
  [dictionary]
  (into #{}
        (mapcat (fn [[k v]]
                  (if (map? v)
                    (map #(keyword (name k) (name %)) (keys v))
                    [k])))
        dictionary))

(defn ns->file
  "Converts a namespace string (e.g. renderer.attribute.impl.d) into its
   corresponding source file, munging dashes to underscores as Clojure does
   when resolving namespaces to paths."
  [ns-str]
  (io/file (src-dir)
           (str (-> ns-str
                    (string/replace "." "/")
                    (string/replace "-" "_"))
                ".cljs")))

(defn key-defined-in-source?
  [source key-name]
  (let [pattern (re-pattern (str "::" (Pattern/quote key-name) "(?![\\w-])"))]
    (boolean (re-find pattern source))))

(defn validate-key-set
  "Compares a language's keys against the base keys, returning a report of
   missing and extra keys."
  [base-keys lang-keys]
  (let [missing (sort (map str (set/difference base-keys lang-keys)))
        extra (sort (map str (set/difference lang-keys base-keys)))]
    (cond-> {}
      (seq missing) (assoc :missing-keys missing)
      (seq extra) (assoc :extra-keys extra))))

(defn validate-source-keys
  "Verifies that every namespaced base key exists as a `::key` literal in its
   corresponding source namespace file. Returns a seq of issue descriptions."
  [base-keys]
  (->> base-keys
       (filter qualified-keyword?)
       (sort-by str)
       (keep (fn [k]
               (let [ns-str (namespace k)
                     key-name (name k)
                     file (ns->file ns-str)]
                 (cond
                   (not (.exists ^File file))
                   (str "  - " k " -> namespace file not found: " file)

                   (not (key-defined-in-source? (slurp file) key-name))
                   (str "  - " k " -> `::" key-name "` not found in "
                        file)))))))

(defn compute-key-set-issues
  "Compares each non-base language file's keys against the base keys.
   Returns a map of lang-id -> report for languages with issues."
  [base-keys other-files]
  (into {}
        (keep (fn [f]
                (let [lang-keys (dictionary->keys (read-lang f))
                      report (validate-key-set base-keys lang-keys)]
                  (when (seq report)
                    [(lang-id f) report]))))
        other-files))

(defn entry-value
  "Formats a base value for display: strings as-is, everything else via
   `pr-str`."
  [v]
  (if (string? v) v (pr-str v)))

(defn key-anchor
  "Returns the closest preceding entry of a namespace (in base order) that is
   already present in the language file, for use as an insertion anchor."
  [present entries target]
  (first (filter #(contains? present (name (first %)))
                 (reverse (take-while #(not= (name (first %)) (name target))
                                      entries)))))

(defn ns-anchor
  "Returns the closest preceding namespace (in base order) that exists in the
   language file, for use as an anchor when inserting a whole new
   namespace."
  [base-dictionary lang-dictionary ns-key]
  (first (filter #(contains? lang-dictionary %)
                 (reverse (take-while #(not= % ns-key)
                                      (keys base-dictionary))))))

(defn missing-groups
  "Groups the base keys missing from a language dictionary by namespace,
   preserving base file order. Namespace groups are
   {:ns-k ns-key :present <set of key names> :missing [[key value] ...]};
   top-level keys that aren't namespace maps produce
   {:scalar key :value value}."
  [base-dictionary lang-dictionary]
  (into []
        (mapcat (fn [k]
                  (let [base-value (k base-dictionary)]
                    (if (map? base-value)
                      (let [lang-ns (k lang-dictionary)
                            present (when (map? lang-ns)
                                      (set (map name (keys lang-ns))))
                            present? (if present
                                       #(contains? present (name (first %)))
                                       (constantly false))
                            missing (filter (complement present?)
                                            (seq base-value))]
                        (when (seq missing)
                          [{:ns-k k
                            :present (or present #{})
                            :missing missing}]))
                      (when-not (contains? lang-dictionary k)
                        [{:scalar k
                          :value base-value}])))))
        (keys base-dictionary)))

(defn missing-key-lines
  "Returns the report lines for one missing key of a namespace group."
  [present entries k v]
  [(str "    " (pr-str k)
        (if-let [a (key-anchor present entries k)]
          (str " (insert after " (pr-str (first a)) ")")
          " (at start)"))
   (str "      en-US: " (entry-value v))])

(defn ns-group-header
  "Returns the header line for a namespace group, noting where a new
   namespace map should be inserted when the language file lacks it."
  [base-dictionary lang-dictionary ns-k]
  (if (ns-k lang-dictionary)
    (str "  " (pr-str ns-k))
    (str "  " (pr-str ns-k)
         (if-let [a (ns-anchor base-dictionary lang-dictionary ns-k)]
           (str " (new namespace, insert after " (pr-str a) ")")
           " (new namespace, at start)"))))

(defn print-ns-group
  "Prints the missing keys of one namespace with their insertion anchors."
  [base-dictionary lang-dictionary {:keys [ns-k present missing]}]
  (println (ns-group-header base-dictionary lang-dictionary ns-k))
  (let [entries (seq (ns-k base-dictionary))]
    (run! println (mapcat (fn [[k v]] (missing-key-lines present entries k v))
                          missing))))

(defn print-scalar-group
  "Prints a missing top-level key and its English value."
  [{:keys [scalar value]}]
  (println (str "  " (pr-str scalar) " (top-level key)"))
  (println (str "    en-US: " (entry-value value))))

(defn print-extra-keys
  "Prints keys found in a language file that are absent from the base file."
  [extra]
  (when (seq extra)
    (println (str "  Extra keys (not in " base-lang ".edn):"))
    (run! #(println "  -" %) extra)))

(defn print-file-report
  "Prints the missing and extra keys of one language file relative to the
   base dictionary. Returns true if anything was printed."
  [base-dictionary ^File f]
  (let [lang-dictionary (read-lang f)
        base-keys (dictionary->keys base-dictionary)
        groups (missing-groups base-dictionary lang-dictionary)
        extra (sort (map str (set/difference (dictionary->keys lang-dictionary)
                                             base-keys)))]
    (when (or (seq groups) (seq extra))
      (println (str (lang-id f) ":"))
      (run! (fn [g]
              (if-let [scalar (:scalar g)]
                (print-scalar-group {:scalar scalar
                                     :value (:value g)})
                (print-ns-group base-dictionary lang-dictionary g)))
            groups)
      (print-extra-keys extra)
      (println)
      true)))

(defn base-file [] (io/file (lang-dir) (str base-lang ".edn")))

(defn other-lang-files
  []
  (remove #(= base-lang (lang-id %)) (lang-files (lang-dir))))

(defn print-report
  []
  (let [base-dictionary (read-lang (base-file))
        other-files (other-lang-files)]
    (println "Missing keys relative to" (str base-lang ".edn") ":")
    (println)
    (if (some true?
              (doall (map #(print-file-report base-dictionary %) other-files)))
      (println "Add the reported keys to the corresponding language files.")
      (println (str "All " (count other-files) " language files match "
                    base-lang ".edn: nothing to translate.")))))

(defn print-source-issues
  [source-issues]
  (if (seq source-issues)
    (do
      (println "Base keys missing from their source namespace:")
      (run! println source-issues))
    (println "All base keys exist in their source namespaces.")))

(defn print-key-set-issues
  [issues]
  (if (seq issues)
    (run! (fn [[lang {:keys [missing-keys extra-keys]}]]
            (println (str lang ":"))
            (when (seq missing-keys)
              (println "  Missing keys:")
              (run! #(println "  -" %) missing-keys))
            (when (seq extra-keys)
              (println "  Extra keys:")
              (run! #(println "  -" %) extra-keys))
            (println))
          issues)
    (println "All language files match" (str base-lang ".edn") "key set.")))

(defn validate
  []
  (let [base-keys (dictionary->keys (read-lang (base-file)))
        other-files (other-lang-files)
        issues (sort (compute-key-set-issues base-keys other-files))
        source-issues (validate-source-keys base-keys)]
    (println "Validating translations against" (str base-lang ".edn") "...")
    (println)
    (print-key-set-issues issues)
    (print-source-issues source-issues)
    (when (or (seq issues) (seq source-issues))
      (System/exit 1))))

(defn -main
  [& args]
  (if (some #{"--report" "report"} args)
    (print-report)
    (validate)))
