(ns renderer.shell.impl.python
  (:require
   ["codemirror/mode/python/python.js"]
   [camel-snake-kebab.core :as camel-snake-kebab]
   [clojure.string :as string]
   [goog.html.legacyconversions :refer [trustedResourceUrlFromString]]
   [goog.net.jsloader :refer [safeLoad]]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.hierarchy :as hierarchy]
   [renderer.shell.events :as-alias shell.events]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.reepl.replumb :as shell.reepl.replumb]
   [renderer.shell.subs :as-alias shell.subs]
   [user :as user]))

(hierarchy/derive! :python ::shell.hierarchy/language)

(defn help
  ([]
   (doseq [x (sort-by str (vals (ns-publics 'user)))]
     (help (:name (meta x)))))
  ([command]
   (if-let [f (get (ns-publics 'user) (symbol command))]
     (print (camel-snake-kebab/->snake_case_string (:name (meta f)))
            " - "
            (:doc (meta f)))
     (println "Command not found:" command))))

(defn expose-command-to-global-namespace
  [pyodide command]
  (let [fn-val @command
        wrapper (fn [& args]
                  (apply fn-val (map #(-> (if (fn? (.-toJs ^js %))
                                            (.toJs ^js %)
                                            %)
                                          (js->clj :keywordize-keys true))
                                     args)))]
    (.set pyodide.globals
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

               (set! user/help help)

               (-> (.runPythonAsync pyodide "import js")
                   (.then #(rf/dispatch on-success)))))

      (.catch (fn [error]
                (rf/dispatch (conj on-error error))))))

(defmethod shell.hierarchy/init :python
  [params]
  (let [loader (-> "/pyodide/pyodide.js"
                   (trustedResourceUrlFromString)
                   (safeLoad))]
    (.addCallback ^goog.net.jsloader loader #(load-pyodide params))))

(defmethod shell.hierarchy/help :python
  [_language]
  (println "The JavaScript scope can be accessed from Python using the js"
           "module. For example, you can access the document object using"
           "`js.document`.")
  (println "Type `help()` to see a list of commands."))

(defmethod shell.hierarchy/evaluate :python
  [_language s]
  (str "(js/pyodide.runPython \""
       (-> s
           (string/replace "\\" "\\\\")
           (string/replace "\"" "\\\""))
       "\")"))

(defmethod shell.hierarchy/codemirror-options :python
  [_language]
  {:mode "python"})

(defmethod shell.hierarchy/completion :python
  [_language s]
  (when (zero? (.indexOf s "js."))
    (shell.reepl.replumb/js-completion (.slice s 3) "js.")))

(rf/dispatch [::action.events/register-action
              {:id :shell-language/python
               :icon "python"
               :label [::label "Python"]
               :event [::shell.events/activate-language :python]
               :active [::shell.subs/active-language? :python]}])
