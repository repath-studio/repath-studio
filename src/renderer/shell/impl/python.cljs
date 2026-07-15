(ns renderer.shell.impl.python
  (:require
   [goog.html.legacyconversions :refer [trustedResourceUrlFromString]]
   [goog.net.jsloader :refer [safeLoad]]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.hierarchy :as hierarchy]
   [renderer.shell.events :as-alias shell.events]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.subs :as-alias shell.subs]))

(hierarchy/derive! :python ::shell.hierarchy/language)

(defn load-pyodide
  [language {:keys [on-success on-error]}]
  (-> (js/loadPyodide)
      (.then (fn [^js pyodide]
               (aset js/window "pyodide" pyodide)
               (.runPython pyodide "import sys, js")

               (doseq [command (vals (ns-publics 'user))]
                 (.set pyodide.globals
                       (str (:name (meta command)))
                       (.-val command)))

               (rf/dispatch (conj on-success language))))
      (.catch (fn [error]
                (rf/dispatch (conj on-error language error))))))

(defmethod shell.hierarchy/init :python
  [language params]
  (let [loader (-> "/pyodide/pyodide.js"
                   (trustedResourceUrlFromString)
                   (safeLoad))]
    (.addCallback ^goog.net.jsloader loader #(load-pyodide language params))))

(defmethod shell.hierarchy/help :python
  [_language]
  (print "The JavaScript scope can be accessed from Python using the js module."
         "For example, you can access the document object using `js.document`.")
  (print "Type `js.help()` to see a list of commands."))

(defmethod shell.hierarchy/evaluate :python
  [_language s]
  (str "(js/pyodide.runPython \"" s "\")"))

(defmethod shell.hierarchy/codemirror-mode :python
  [_language]
  "python")

(rf/dispatch [::action.events/register-action
              {:id :shell-language/python
               :label [::label "Python"]
               :event [::shell.events/activate-language :python]
               :active [::shell.subs/active-language? :python]}])
