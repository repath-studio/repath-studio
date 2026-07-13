(ns renderer.reepl.impl.python
  (:require
   [goog.html.legacyconversions :refer [trustedResourceUrlFromString]]
   [goog.net.jsloader :refer [safeLoad]]
   [re-frame.core :as rf]
   [renderer.reepl.hierarchy :as reepl.hierarchy]))

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
               (rf/dispatch [:renderer.reepl.events/language-loaded])))))

(defmethod reepl.hierarchy/init :python
  []
  (let [loader (-> "https://cdn.jsdelivr.net/pyodide/v314.0.2/full/pyodide.js"
                   (trustedResourceUrlFromString)
                   (safeLoad))]
    (.addCallback ^goog.net.jsloader loader load-pyodide)))
