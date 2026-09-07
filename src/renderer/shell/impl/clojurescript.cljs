(ns renderer.shell.impl.clojurescript
  (:require
   ["@nextjournal/lang-clojure" :refer [clojure clojureLanguage]]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.hierarchy :as hierarchy]
   [renderer.shell.events :as-alias shell.events]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.reepl.sci :as shell.reepl.sci]
   [renderer.shell.subs :as-alias shell.subs]
   [user]))

(hierarchy/derive! :cljs ::shell.hierarchy/language)

(defn dsl-namespaces
  []
  (or (get @shell.reepl.sci/dsl-data :namespaces)
      (let [publics (ns-publics 'user)]
        {"user" (zipmap (map name (keys publics))
                        (repeat (count publics) {}))})))

(defn completion
  [text]
  (let [text (str text)
        [only-ns text] (if-not (= -1 (.indexOf text "/"))
                         (.split text "/")
                         [nil text])
        matches? #(and
                   (= -1 (.indexOf (str %) "t_cljs$core"))
                   (< -1 (.indexOf (str %) text)))
        current-ns-str (str @shell.reepl.sci/current-ns-ref)
        namespaces (if (some? @shell.reepl.sci/dsl-data)
                     (get @shell.reepl.sci/dsl-data :namespaces)
                     (dsl-namespaces))
        aliases (get-in @shell.reepl.sci/dsl-data [:aliases current-ns-str] {})
        only-ns-str (when only-ns
                      (or (get aliases only-ns)
                          only-ns))
        replace-name (fn [sym]
                       (if (or (= "cljs.core" (namespace sym))
                               (= current-ns-str (namespace sym)))
                         (name sym)
                         (str sym)))
        sources (if only-ns-str
                  [[only-ns-str only-ns-str]]
                  (for [ns* (keys namespaces)]
                    [(or (get aliases ns*) ns*) ns*]))
        names (set (map str (keys namespaces)))
        defs (->> sources
                  (sort-by second (partial shell.reepl.sci/compare-ns
                                           current-ns-str))
                  (mapcat (fn [[name* ns*]]
                            (sort (map #(symbol name* (str %))
                                       (filter matches?
                                               (keys (get namespaces ns*)))))))
                  (map #(vector % (replace-name %) (replace-name %)))
                  (sort-by #(name (first %))
                           (partial shell.reepl.sci/compare-completion text)))]
    (->> (filter matches? names)
         (map #(vector % (str %) (str %)))
         (concat (take config/max-shell-completions defs))
         (vec))))

(defmethod shell.hierarchy/init :cljs
  [{:keys [on-success]}]
  (rf/dispatch on-success))

(defmethod shell.hierarchy/help :cljs
  [_language command]
  (if-let [f (get (ns-publics 'user) (symbol command))]
    (print (:name (meta f)) " - " (:doc (meta f)))
    (println "Command not found:" command)))

(defmethod shell.hierarchy/welcome :cljs
  [_language]
  (println "Global javascript objects and functions are accessible using the js"
           "namespace (e.g. `js/document`).")
  (println "Type `(help)` to see a list of commands."))

(defmethod shell.hierarchy/evaluate :cljs
  [_language s]
  s)

(defmethod shell.hierarchy/codemirror-options :cljs
  [_language]
  {:extensions [(clojure)]})

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
    (shell.reepl.sci/process-doc s)))

(defmethod shell.hierarchy/show-error :cljs
  [_language v]
  (str "Error: " (:cause v)))

(rf/dispatch [::action.events/register-action
              {:id :shell-language/clojurescript
               :icon "clojurescript"
               :label [::label "ClojureScript"]
               :event [::shell.events/activate-language :cljs]
               :active [::shell.subs/active-language? :cljs]}])
