(ns build.shell-dsl-generator
  "Shadow-cljs build hook that generates a docs/completions data file for the
   shell from the CLJS compiler's analysis.

   The generated file (resources/public/js/shell-dsl.edn) is loaded at
   runtime by renderer.shell.reepl.sci and provides completions and docs for
   the shell DSL (`user` namespace) and the namespaces it requires, keeping
   them available regardless of the build's optimization level.

   The hook runs at the `:compile-finish` stage, i.e. after compilation has
   populated the `:cljs.analyzer/namespaces` slot of the compiler env held
   in the build state under `:compiler-env`.
   Namespace analyses hold their vars under `:defs` (and macros under
   `:macros`) and their requires under `:requires` / `:require-macros` as
   local-name -> namespace-name maps."
  (:require
   [clojure.java.io :as io]))

(defn output-file! []
  (io/file "resources" "public" "js" "shell-dsl.edn"))

(defn- arglists*
  "Unwraps the quoted arglists form of var metadata."
  [arglists]
  (cond-> arglists
    (and (seq? arglists) (= 'quote (first arglists)))
    second))

(defn- var-docs
  "A {:doc doc :arglists arglists} map for a public var analysis entry or
   var metadata, or nil if the var is private or has nothing to document."
  [v]
  (when (and (map? v)
             (not (:private v)))
    (let [doc (or (:doc v)
                  (get-in v [:meta :doc]))
          arglists (arglists* (or (:arglists v)
                                  (get-in v [:meta :arglists])))]
      (cond-> {}
        (some? doc)
        (assoc :doc doc)
        (some? arglists)
        (assoc :arglists arglists)))))

(defn- ns-var-docs
  "A map of var name to {:doc doc :arglists arglists} for the public vars
   and macros of a namespace's analysis."
  [ns-ana]
  (reduce
   (fn [docs slot]
     (reduce
      (fn [docs [nm v]]
        (if-let [doc (var-docs v)]
          (assoc docs (name (name nm)) doc)
          docs))
      docs
      (seq (get ns-ana slot))))
   {}
   [:defs :macros]))

(defn- ana-aliases
  "A map of local name (alias or namespace name) to namespace name from the
   requires of a namespace's analysis."
  [ns-ana]
  (into {}
        (map (fn [[k v]] [(str k) (str v)]))
        (merge (get ns-ana :requires)
               (get ns-ana :require-macros))))

(defn- clj-core-docs
  "Fallback docs for `clojure.core` from the CLJ runtime, used when the CLJS
   analysis of core is not available in the compiler env."
  []
  (into {}
        (keep (fn [v]
                (when-let [doc (var-docs (meta v))]
                  [(.getName ^clojure.lang.Var v) doc])))
        (vals (ns-map 'clojure.core))))

(defn- ana-for
  "The namespace analysis for `ns*` from the analyzer namespaces map,
   handling the `clojure.core`/`cljs.core` aliasing."
  [namespaces ns*]
  (if (= "clojure.core" ns*)
    (or (get namespaces 'clojure.core)
        (get namespaces 'cljs.core))
    (get namespaces (symbol ns*))))

(defn- ns-entry-docs
  "The var docs map for namespace `ns*`, falling back to the CLJ runtime
   for `clojure.core`."
  [namespaces ns*]
  (let [docs (when-let [ana (ana-for namespaces ns*)]
               (ns-var-docs ana))]
    (or docs
        (when (= "clojure.core" ns*)
          (clj-core-docs)))))

(defn- include-nss
  "The namespaces to include in the generated file: `user`, the namespaces
   it requires and `clojure.core`."
  [namespaces]
  (into (conj (into #{} (vals (ana-aliases (get namespaces 'user)))) "user")
        '("clojure.core")))

(defn- get-aliases
  [namespaces include]
  (into {}
        (keep (fn [ns*]
                (when-let [a (and (not= "clojure.core" ns*)
                                  (ana-aliases
                                   (ana-for namespaces
                                            ns*)))]
                  (when (seq a) [ns* a]))))
        include))

(defn generate!
  "Generates the DSL data file from the CLJS compiler env in `state`."
  [state]
  (let [namespaces (get-in state [:compiler-env :cljs.analyzer/namespaces])
        include (include-nss namespaces)
        ns-docs (into {}
                      (keep (fn [ns*]
                              (when-let [docs (ns-entry-docs
                                               namespaces ns*)]
                                (when (seq docs) [ns* docs]))))
                      include)
        aliases (get-aliases namespaces include)]
    (io/make-parents (output-file!))
    (spit (output-file!) (pr-str {:aliases aliases
                                  :namespaces ns-docs}))
    (println (str "[shell-dsl-generator] Generated "
                  (.getPath (output-file!))
                  " (" (count (get ns-docs "user")) " user vars, "
                  (count ns-docs) " namespaces)"))))

(defn ^:export hook!
  {:shadow.build/stages #{:compile-finish}}
  [build-state & _args]
  (case (:shadow.build/stage build-state)
    :compile-finish (do (generate! build-state)
                        build-state)))
