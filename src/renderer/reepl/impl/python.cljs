(ns renderer.reepl.impl.python
  (:require
   [goog.html.legacyconversions :refer [trustedResourceUrlFromString]]
   [goog.net.jsloader :refer [safeLoad]]
   [re-frame.core :as rf]
   [renderer.hierarchy :as hierarchy]
   [renderer.reepl.hierarchy :as reepl.hierarchy]))

(hierarchy/derive! :py ::reepl.hierarchy/language)

(defn load-pyodide
  []
  (-> (js/loadPyodide)
      (.then (fn [^js pyodide]
               (aset js/window "pyodide" pyodide)
               (.runPython pyodide "import sys, js")
               (print "Welcome to your python REPL!")
               (print "")
               (print "You can create or modify shapes using the command line.")
               (print "Type `js.help()` to see a list of commands.")
               (rf/dispatch [:renderer.reepl.events/language-load-success])))
      (.catch (fn [error]
                (rf/dispatch [:renderer.reepl.events/language-load-error
                              error])))))

(defmethod reepl.hierarchy/init :py
  []
  (let [loader (-> "/pyodide/pyodide.js"
                   (trustedResourceUrlFromString)
                   (safeLoad))]
    (.addCallback ^goog.net.jsloader loader load-pyodide)))

(defmethod reepl.hierarchy/evaluate :py
  [_language s]
  (str "(js/pyodide.runPython \"" s "\")"))
