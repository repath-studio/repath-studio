(ns build.translation-validator
  "Validates translation files under src/lang against the en-US base file.

   Checks that every language file defines exactly the same set of keys as
   en-US.edn, and that every base key exists as a `::key` in the ClojureScript
   namespace file it is declared under."
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

(defn -main
  [& _args]
  (let [dir (lang-dir)
        base-file (io/file dir (str base-lang ".edn"))
        base-dictionary (read-lang base-file)
        base-keys (dictionary->keys base-dictionary)
        other-files (remove #(= base-lang (lang-id %)) (lang-files dir))
        key-set-issues (compute-key-set-issues base-keys other-files)
        source-issues (validate-source-keys base-keys)]
    (println "Validating translations against" (str base-lang ".edn") "...")
    (println)
    (if (seq key-set-issues)
      (doseq [[lang {:keys [missing-keys extra-keys]}] (sort key-set-issues)]
        (println (str lang ":"))
        (when (seq missing-keys)
          (println "  Missing keys:")
          (doseq [k missing-keys] (println "  -" k)))
        (when (seq extra-keys)
          (println "  Extra keys:")
          (doseq [k extra-keys] (println "  -" k)))
        (println))
      (println "All language files match" (str base-lang ".edn") "key set."))
    (if (seq source-issues)
      (do
        (println "Base keys missing from their source namespace:")
        (doseq [issue source-issues] (println issue)))
      (println "All base keys exist in their source namespaces."))
    (when (or (seq key-set-issues) (seq source-issues))
      (System/exit 1))))
