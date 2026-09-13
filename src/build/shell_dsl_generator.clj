(ns build.shell-dsl-generator
  "Shadow-cljs build hook that generates a CLJS data namespace holding the
   shell's completions and docs.

   It parses the `user` namespace source (src/user.cljs) to extract the doc
   and arglist of each public command, reads the `clojure.core` docs from the
   Clojure runtime, and derives the namespace aliases from the `user`
   `:require` form. The result is written to src/generated/shell_dsl.cljs, which
   the renderer shell requires directly (see renderer.shell.reepl.sci), so
   the data is compiled in rather than fetched at runtime.

   The hook runs at the `:configure` and `:compile-prepare` stages, i.e.
   before compilation, and only rewrites the file when its content changes."
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [clojure.string :as string]))

(defn source-file-path! []
  (io/file "src" "user.cljs"))

(defn output-file-path! []
  (io/file "src" "generated" "shell_dsl.cljs"))

(def ^:private backslash (char 92))
(def ^:private quote-char (char 34))
(def ^:private colon (char 58))

(defn- readable-source
  "Returns `s` with any `::` outside of string literals rewritten to `:` so
   the file can be read by the Clojure reader (`::` is a ClojureScript reader
   macro). String contents and escape sequences are preserved verbatim."
  [s]
  (let [n (count s)]
    (loop [i 0 in-string? false sb (StringBuilder. n)]
      (if (>= i n)
        (.toString sb)
        (let [c (nth s i)
              c2 (when (< (inc i) n) (nth s (inc i)))]
          (cond
            (and in-string? (= c backslash))
            (do (.append sb c)
                (when c2 (.append sb c2))
                (recur (if c2 (+ i 2) (inc i)) true sb))

            (and in-string? (= c quote-char))
            (do (.append sb c)
                (recur (inc i) false sb))

            in-string?
            (do (.append sb c)
                (recur (inc i) true sb))

            (and (= c colon) (= c2 colon))
            (do (.append sb colon)
                (recur (+ i 2) false sb))

            (= c quote-char)
            (do (.append sb c)
                (recur (inc i) true sb))

            :else
            (do (.append sb c)
                (recur (inc i) false sb))))))))

(defn- top-level-forms
  "Reads and returns the top-level forms of `source`."
  [source]
  (with-open [r (java.io.PushbackReader. (java.io.StringReader. source))]
    (loop [forms ()]
      (let [res (try [(read r) nil]
                     (catch Exception e [::read-error (ex-message e)]))]
        (cond
          (= (first res) ::read-error)
          (if (string/includes? (second res) "EOF while reading")
            (vec (reverse forms))
            (throw (ex-info (str "Error reading form: " (second res)) {})))

          :else
          (recur (conj forms (first res))))))))

(defn- arglists*
  "Unwraps the quoted arglists form of var metadata."
  [arglists]
  (cond-> arglists
    (and (seq? arglists) (= 'quote (first arglists)))
    second))

(defn- quoted-arglists
  "Arglists wrapped in a `quote` form so the generated CLJS source reads
   them back as a list rather than compiling them as code."
  [arglists]
  (list (quote quote) arglists))

(defn- var-docs
  "A {:doc doc :arglists arglists} map for a public var's metadata, or nil
   when the var is private or has nothing to document."
  [meta-map]
  (when (and (map? meta-map)
             (not (:private meta-map)))
    (let [doc (:doc meta-map)
          arglists (arglists* (:arglists meta-map))]
      (cond-> {}
        (some? doc)
        (assoc :doc doc)
        (some? arglists)
        (assoc :arglists (quoted-arglists arglists))))))

(defn- clj-core-docs
  "A map of var name to {:doc doc :arglists arglists} for the public vars of
   `clojure.core`, from the Clojure runtime."
  []
  (into (sorted-map)
        (keep (fn [[sym v]]
                (when-let [doc (var-docs (meta v))]
                  [(name sym) doc])))
        (ns-map 'clojure.core)))

(defn- private-var?
  [name*]
  (or (string/starts-with? (str name*) "-")
      (:private (meta name*))))

(defn- defn-arglists
  "A list of arglist vectors from a defn's body forms, in order, or nil when
   there are none. A single-arity form's arglist is a vector; each arity of a
   multi-arity form is a list headed by its arglist vector."
  [arity-forms]
  (cond (empty? arity-forms) nil
        (vector? (first arity-forms)) (list (first arity-forms))
        (seq? (first arity-forms)) (apply list (map first arity-forms))
        :else nil))

(defn- def-docs
  "A {:doc doc :arglists arglists} map for a top-level `def`/`defn` form
   naming a public var, or nil otherwise."
  [form]
  (when (and (seq? form) (#{'def 'defn} (first form)))
    (let [name* (second form)
          after-name (nthnext form 2)
          doc (when (string? (first after-name)) (first after-name))
          after-doc (if doc (next after-name) after-name)
          after-meta (if (map? (first after-doc)) (next after-doc) after-doc)
          arglists (when (= 'defn (first form))
                     (defn-arglists after-meta))]
      (when (and (symbol? name*) (not (private-var? name*)) (or doc arglists))
        (cond-> {}
          (some? doc)
          (assoc :doc doc)
          (some? arglists)
          (assoc :arglists (quoted-arglists arglists)))))))

(defn- user-docs
  "A map of var name to {:doc doc :arglists arglists} for the public top-level
   `def`/`defn` vars in `forms`."
  [forms]
  (into (sorted-map)
        (keep (fn [form]
                (when-let [doc (def-docs form)]
                  [(name (second form)) doc])))
        forms))

(defn- require-aliases
  "A map of local name (alias or namespace name) to namespace name from the
   `:require` option of the `ns` form."
  [ns-form]
  (let [require-opt (some #(when (= :require (first %)) %) (nthnext ns-form 2))
        requires (seq (next require-opt))]
    (into (sorted-map)
          (mapcat (fn [req]
                    (when (and (sequential? req) (symbol? (first req)))
                      (let [ns-str (str (first req))
                            alias* (when (#{:as :as-alias} (first (next req)))
                                     (second (next req)))]
                        (if alias*
                          [[ns-str ns-str] [(str alias*) ns-str]]
                          [[ns-str ns-str]]))))
                  requires))))

(defn- ns-docs
  [forms]
  (sorted-map "clojure.core" (clj-core-docs)
              "user" (user-docs forms)))

(defn generate-source
  [aliases namespaces]
  (str ";; ============================================================\n"
       ";; AUTO-GENERATED — DO NOT EDIT\n"
       ";; Generated by build.shell-dsl-generator from src/user.cljs\n"
       ";; ============================================================\n"
       "(ns generated.shell-dsl)\n"
       "\n"
       "(def aliases\n"
       "  "
       (binding [pprint/*print-right-margin* 120]
         (with-out-str (pprint/pprint aliases)))
       ")\n"
       "\n"
       "(def namespaces\n"
       "  "
       (binding [pprint/*print-right-margin* 120]
         (with-out-str (pprint/pprint namespaces)))
       ")\n"))

(defn generate!
  []
  (let [forms (top-level-forms (readable-source (slurp (source-file-path!))))
        aliases (sorted-map "user" (require-aliases (first forms)))
        namespaces (ns-docs forms)
        output (output-file-path!)
        new-source (generate-source aliases namespaces)
        changed? (or (not (.exists output))
                     (not= (slurp output) new-source))]
    (when changed?
      (io/make-parents output)
      (spit output new-source)
      (println (str "[shell-dsl-generator] Generated " (.getPath output)
                    " (" (count (get namespaces "user")) " user vars, "
                    (count namespaces) " namespaces)")))
    changed?))

(defn ^:export hook!
  {:shadow.build/stages #{:configure :compile-prepare}}
  [build-state & _args]
  (case (:shadow.build/stage build-state)
    :configure (do (generate!)
                   build-state)
    :compile-prepare (do (generate!)
                         build-state)))
