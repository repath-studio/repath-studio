(ns renderer.shell.impl.python
  (:require
   ["@codemirror/lang-python" :refer [python]]
   ["@codemirror/state" :refer [EditorState]]
   [camel-snake-kebab.core :as camel-snake-kebab]
   [clojure.string :as string]
   [goog.html.legacyconversions :refer [trustedResourceUrlFromString]]
   [goog.net.jsloader :refer [safeLoad]]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.hierarchy :as hierarchy]
   [renderer.shell.events :as-alias shell.events]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.reepl.sci :as shell.reepl.sci]
   [renderer.shell.subs :as-alias shell.subs]
   [renderer.utils.extra :refer [log]]
   [user]))

(hierarchy/derive! :python ::shell.hierarchy/language)

(defn expose-command-to-global-namespace
  [pyodide command]
  (let [fn-val @command
        wrapper (fn [& args]
                  (apply fn-val (map #(-> (if (fn? (.-toJs ^js %))
                                            (.toJs ^js %)
                                            %)
                                          (js->clj :keywordize-keys true))
                                     args)))]
    (.set (.-globals ^js pyodide)
          (-> (:name (meta command))
              (camel-snake-kebab/->snake_case_string))
          wrapper)))

(defn load-pyodide
  [{:keys [on-success on-error]}]
  (-> (js/loadPyodide)
      (.then (fn [^js pyodide]
               (aset js/window "pyodide" pyodide)

               ;; Expose all user functions to global namespace.
               (doseq [command (vals (ns-publics 'user))]
                 (expose-command-to-global-namespace pyodide command))

               (-> (.runPythonAsync pyodide "import js")
                   (.then #(rf/dispatch on-success)))))

      (.catch (fn [error]
                (rf/dispatch (conj on-error error))))))

(defmethod shell.hierarchy/init :python
  [params]
  (let [loader (-> "pyodide/pyodide.js"
                   (trustedResourceUrlFromString)
                   (safeLoad))]
    (.addCallback ^goog.net.jsloader loader #(load-pyodide params))))

(defmethod shell.hierarchy/help :python
  [_language command]
  (if-let [f (get (ns-publics 'user) (symbol command))]
    (log [:command (camel-snake-kebab/->snake_case_string (:name (meta f)))]
         " - "
         (first (string/split-lines (:doc (meta f)))))
    (log "Command not found:" command)))

(defmethod shell.hierarchy/welcome :python
  [_language]
  (log "The JavaScript scope can be accessed from Python using the js module."
       "For example, you can access the document object using `"
       [:command "js.document"] "`.")
  (log "Type `" [:command "help()"] "` to see a list of commands."))

(defmethod shell.hierarchy/evaluate :python
  [_language s]
  (str "(js/pyodide.runPython \""
       (-> s
           (string/replace "\\" "\\\\")
           (string/replace "\"" "\\\""))
       "\")"))

(defmethod shell.hierarchy/codemirror-options :python
  [_language]
  {:extensions [(.of EditorState.languageData
                     (fn [] #js [#js {:wordChars "."}]))
                (python)]})

(defmethod shell.hierarchy/parser :python
  [_language]
  (.. (python) -language -parser))

(defmethod shell.hierarchy/completions :python
  [_language s]
  (if (zero? (.indexOf s "js."))
    (shell.reepl.sci/js-completion (.slice s 3) "js.")
    (->> (ns-publics 'user)
         (map (fn [[sym _var]]
                [sym (camel-snake-kebab/->snake_case_string (name sym))]))
         (filter (fn [[_sym snake-name]] (string/starts-with? snake-name s)))
         (sort-by second (partial shell.reepl.sci/compare-completion s))
         (into []))))

(defn- py-arg
  [arg]
  (cond (symbol? arg) (name arg)
        (vector? arg) (str "[" (string/join ", " (map py-arg arg)) "]")
        (and (map? arg) (:as arg)) (str "**" (name (:as arg)))
        :else (pr-str arg)))

(defn- py-arglist
  "Converts a Clojure arglist to a Python-style signature.
   E.g. `[[cx cy] r & {:as attrs}]` becomes `([cx, cy], r, **attrs)`."
  [arglist]
  (let [rest-pos (reduce-kv (fn [i k v] (or i (when (= '& v) k))) nil arglist)
        args (take (or rest-pos (count arglist)) arglist)
        rest-arg (when rest-pos (nth arglist (inc rest-pos)))]
    (str "(" (string/join ", "
                          (concat (map py-arg args)
                                  (when (some? rest-arg)
                                    [(if (map? rest-arg)
                                       (py-arg rest-arg)
                                       (str "*" (name rest-arg)))])))
         ")")))

(defmethod shell.hierarchy/docs :python
  [_language s]
  (when (symbol? s)
    (when-let [f (get (ns-publics 'user) s)]
      (let [m (meta f)]
        (with-out-str
          (shell.reepl.sci/print-language-doc
           {:name (camel-snake-kebab/->snake_case_string (name s))
            :forms (:arglists m)
            :doc (:doc m)}
           py-arglist))))))

(defmethod shell.hierarchy/show-error :python
  [_language v]
  (-> (:cause v)
      (str)
      (string/split "\n")
      (last)))

(rf/dispatch [::action.events/register-action
              {:id :shell-language/python
               :icon "python"
               :label [::label "Python"]
               :event [::shell.events/activate-language :python]
               :active [::shell.subs/active-language? :python]}])
