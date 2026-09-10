(ns renderer.shell.impl.clojurescript
  (:require
   ["@codemirror/state" :refer [EditorState]]
   ["@nextjournal/lang-clojure" :refer [clojure clojureLanguage]]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.hierarchy :as hierarchy]
   [renderer.shell.events :as-alias shell.events]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.reepl.sci :as shell.reepl.sci]
   [renderer.shell.subs :as-alias shell.subs]
   [renderer.utils.extra :refer [log]]
   [user]))

(hierarchy/derive! :cljs ::shell.hierarchy/language)

(defn- dsl-namespaces
  []
  (or (get @shell.reepl.sci/dsl-data :namespaces)
      (let [publics (ns-publics 'user)]
        {"user" (zipmap (map name (keys publics))
                        (repeat (count publics) {}))})))

(defn- matches?
  [s text]
  (let [s (str s)]
    (and (not (string/includes? s "t_cljs$core"))
         (string/includes? s text))))

(defn- ns-sources
  [{:keys [namespaces local-names only-ns only-ns-str current-ns-str]}]
  (let [local-name #(or (get local-names %) %)]
    (->> (if only-ns-str
           [[only-ns only-ns-str]]
           (map (juxt local-name identity)
                (keys namespaces)))
         (sort-by second (partial shell.reepl.sci/compare-ns
                                  current-ns-str)))))

(defn- completion-name
  [name* ns* nm current-ns-str]
  (cond->> nm
    (not (or (= current-ns-str ns*)
             (#{"clojure.core" "cljs.core"} ns*)))
    (str name* "/")))

(defn- ns-completions
  [namespaces text]
  (->> (keys namespaces)
       (remove #(= "clojure.core" %))
       (filter #(matches? % text))
       (sort)
       (sort-by identity (partial shell.reepl.sci/compare-completion text))
       (map #(vector % %))))

(defn- var-completions
  [namespaces sources text current-ns-str]
  (->> sources
       (mapcat (fn [[name* ns*]]
                 (->> (keys (get namespaces ns*))
                      (filter #(matches? % text))
                      (sort)
                      (map (fn [nm]
                             (vector (symbol name* nm)
                                     (completion-name name* ns* nm
                                                      current-ns-str)))))))
       (sort-by #(name (first %))
                (partial shell.reepl.sci/compare-completion text))))

(defn- completion
  [text]
  (let [text (str text)
        [only-ns text] (if (= -1 (.indexOf text "/"))
                         [nil text]
                         (let [parts (.split text "/")]
                           [(first parts)
                            (string/join "/" (rest parts))]))
        only-ns (when (seq only-ns) only-ns)
        current-ns-str (shell.reepl.sci/current-ns)
        namespaces (dsl-namespaces)
        aliases (get-in @shell.reepl.sci/dsl-data [:aliases current-ns-str] {})
        local-names (into {} (map (juxt second first) aliases))
        only-ns-str (when only-ns
                      (or (get aliases only-ns)
                          only-ns))
        sources (ns-sources {:namespaces namespaces
                             :local-names local-names
                             :only-ns only-ns
                             :only-ns-str only-ns-str
                             :current-ns-str current-ns-str})
        defs (var-completions namespaces sources text current-ns-str)
        nss (ns-completions namespaces (or only-ns text))]
    (vec (concat nss defs))))

(defmethod shell.hierarchy/init :cljs
  [{:keys [on-success]}]
  (rf/dispatch on-success))

(defmethod shell.hierarchy/help :cljs
  [_language command]
  (if-let [f (get (ns-publics 'user) (symbol command))]
    (log [:command (str (:name (meta f)))] " - " (:doc (meta f)))
    (log "Command not found:" command)))

(defmethod shell.hierarchy/welcome :cljs
  [_language]
  (log "Global javascript objects and functions are accessible using the `"
       [:command "js/"] "` namespace (e.g. `" [:command "js/document"] "`).")
  (log "Type `" [:command "(help)"] "` to see a list of commands."))

(defmethod shell.hierarchy/evaluate :cljs
  [_language s]
  s)

(defmethod shell.hierarchy/codemirror-options :cljs
  [_language]
  {:extensions [(.of EditorState.languageData
                     (fn [] #js [#js {:wordChars "/.+-*=!<>?"}]))
                (clojure)]})

(defmethod shell.hierarchy/parser :cljs
  [_language]
  (.-parser clojureLanguage))

(defmethod shell.hierarchy/completions :cljs
  [_language s]
  (if (zero? (.indexOf s "js/"))
    (shell.reepl.sci/js-completion (.slice s 3) "js/")
    (completion s)))

(defmethod shell.hierarchy/docs :cljs
  [_language s]
  (when (symbol? s)
    (when-let [doc (shell.reepl.sci/doc-from-sym s)]
      (with-out-str
        (shell.reepl.sci/print-doc doc)))))

(defmethod shell.hierarchy/show-error :cljs
  [_language v]
  (str "Error: " (:cause v)))

(rf/dispatch [::action.events/register-action
              {:id :shell-language/clojurescript
               :icon "clojurescript"
               :label [::label "ClojureScript"]
               :event [::shell.events/activate-language :cljs]
               :active [::shell.subs/active-language? :cljs]}])
