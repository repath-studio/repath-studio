(ns renderer.shell.impl.python
  (:require
   ["@codemirror/lang-python" :refer [python]]
   ["@codemirror/state" :refer [EditorState]]
   [camel-snake-kebab.core :as camel-snake-kebab]
   [clojure.set :as set]
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

(def shell-complete
  (string/join "\n"
               ["def _shell_complete(text):"
                "    import sys, json, jedi, builtins"
                "    p = text.split('.')"
                "    f, pre = p[-1], p[:-1]"
                "    g = dict(globals())"
                "    if pre:"
                "        root = pre[0]"
                "        known = root in g or root in builtins.__dict__"
                "        if not root or not known:"
                "            return '[]'"
                "        if root in builtins.__dict__:"
                "            g[root] = builtins.__dict__[root]"
                "    try:"
                "        interp = jedi.Interpreter(text, [g])"
                "        names = {c.name for c in interp.complete()}"
                "    except Exception:"
                "        names = set()"
                "    if pre:"
                "        names = {n for n in names"
                "                 if n[:2] != '__' and n.startswith(f)}"
                "    else:"
                "        names = {n for n in names if n[:1] != '_'}"
                "        names |= set(sys.stdlib_module_names)"
                "        names = {n for n in names if n.startswith(f)}"
                "    b = text[:len(text) - len(f)]"
                "    return json.dumps(sorted(b + n for n in names))"]))

(def doc-target
  (string/join "\n"
               ["def _doc_target(name, g):"
                "    import sys, builtins, types"
                "    root = name.split('.')[0]"
                "    if not root:"
                "        return None"
                "    if root in builtins.__dict__:"
                "        return builtins.__dict__[root]"
                "    obj = g.get(root)"
                "    if obj is None:"
                "        obj = sys.modules.get(root)"
                "    if isinstance(obj, types.ModuleType):"
                "        return obj"
                "    return None"]))

(def doc-from-source
  (string/join "\n"
               ["def _doc_from_source(name):"
                "    import importlib.util, ast"
                "    try:"
                "        spec = importlib.util.find_spec(name)"
                "        src = None"
                "        if spec and spec.loader:"
                "            src = spec.loader.get_source(name)"
                "        if isinstance(src, str):"
                "            return ast.get_docstring(ast.parse(src)) or ''"
                "    except Exception:"
                "        pass"
                "    return ''"]))

(def shell-doc
  (string/join "\n"
               ["def _shell_doc(name):"
                "    import json, jedi"
                "    g = dict(globals())"
                "    root = name.split('.')[0]"
                "    obj = _doc_target(name, g)"
                "    if obj is None:"
                "        doc = ''"
                "        if root and '.' not in name:"
                "            doc = _doc_from_source(root)"
                "        if not doc:"
                "            return ''"
                "        return json.dumps({'name': name,"
                "                           'type': 'module',"
                "                           'signature': name + '()',"
                "                           'doc': doc})"
                "    g[root] = obj"
                "    try:"
                "        interp = jedi.Interpreter(name, [g])"
                "        names = interp.infer(1, len(name))"
                "    except Exception:"
                "        names = []"
                "    if not names:"
                "        return ''"
                "    n = names[0]"
                "    sig = ''"
                "    try:"
                "        sigs = n.get_signatures()"
                "        if sigs:"
                "            sig = sigs[0].to_string()"
                "    except Exception:"
                "        pass"
                "    doc = n.docstring() or ''"
                "    if sig and doc.startswith(sig + '\\n'):"
                "        doc = doc[len(sig) + 1:].lstrip()"
                "    return json.dumps({'name': name, 'type': n.type,"
                "                       'signature': sig, 'doc': doc})"]))

(def aliases
  {"del" "delete"
   "raise" "bring_forward"
   "lower" "send_forward"})

(defn- js->py-name
  [name-str]
  (let [snake (camel-snake-kebab/->snake_case_string name-str)]
    (or (get aliases snake)
        snake)))

(defn- py->js-name
  [name-str]
  (let [snake (camel-snake-kebab/->kebab-case-string name-str)]
    (or (get (set/map-invert aliases) snake)
        snake)))

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
          (js->py-name (:name (meta command)))
          wrapper)))

(defn load-pyodide
  [{:keys [on-success on-error]}]
  (-> (js/loadPyodide)
      (.then (fn [^js pyodide]
               (aset js/window "pyodide" pyodide)

               ;; Expose all user functions to global namespace.
               (doseq [command (vals (ns-publics 'user))]
                 (expose-command-to-global-namespace pyodide command))

               (-> (.loadPackage ^js pyodide "jedi")
                   (.then (fn []

                            (-> (.runPythonAsync pyodide
                                                 (->> ["import js"
                                                       shell-complete
                                                       doc-target
                                                       doc-from-source
                                                       shell-doc
                                                       "_shell_complete('')"
                                                       "_shell_doc('')"]
                                                      (string/join "\n")))
                                (.then #(rf/dispatch on-success))))))))

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
  (if-let [f (get (ns-publics 'user) (symbol (py->js-name command)))]
    (log [:command (js->py-name (:name (meta f)))]
         " - "
         (first (string/split-lines (:doc (meta f)))))
    (log "Command not found:" command)))

(defmethod shell.hierarchy/welcome :python
  [_language]
  (log "The JavaScript scope can be accessed from Python using the "
       [:command "js"] " module " "(e.g `" [:command "js.document"] "`).")
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
                     (fn [] #js [#js {:wordChars ".$"}]))
                (python)]})

(defmethod shell.hierarchy/parser :python
  [_language]
  (.. (python) -language -parser))

(defn- user-by-snake-name
  []
  (->> (ns-publics 'user)
       (map (fn [[sym _var]] [(js->py-name (name sym)) sym]))
       (into {})))
(defn- pyodide-completions
  [text]
  (.set (.-globals js/pyodide) "_shell_text" text)
  (some->> (.runPython js/pyodide "_shell_complete(_shell_text)")
           (.parse js/JSON)
           (js->clj)
           (into [])))

(defmethod shell.hierarchy/completions :python
  [_language s]
  (let [by-name (user-by-snake-name)
        texts (or (pyodide-completions s)
                  (->> (keys by-name)
                       (filter #(string/starts-with? % s))))
        cmp (partial shell.reepl.sci/compare-completion s)]
    (->> texts
         (map (fn [t] [(or (by-name t) t) t]))
         (sort-by second cmp)
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

(defn- py-jedi-docs
  [n]
  (let [r (try (.runPython js/pyodide
                           (str "_shell_doc(" (js/JSON.stringify n) ")"))
               (catch :default _ nil))]
    (when (and (string? r) (seq r))
      (let [d (js->clj (.parse js/JSON r) :keywordize-keys true)]
        (with-out-str
          (shell.reepl.sci/print-language-doc
           {:name (if (and (seq (:signature d))
                           (= (:type d) "function"))
                    (:signature d)
                    (:name d))
            :doc (:doc d)}
           identity))))))

(defmethod shell.hierarchy/docs :python
  [_language s]
  (cond
    (string? s)
    (py-jedi-docs s)

    (symbol? s)
    (when-let [f (get (ns-publics 'user) s)]
      (let [m (meta f)]
        (with-out-str
          (shell.reepl.sci/print-language-doc
           {:name (js->py-name (name s))
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
