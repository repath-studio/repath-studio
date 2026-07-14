(ns renderer.shell.impl.clojurescript
  (:require
   [re-frame.core :as rf]
   [renderer.hierarchy :as hierarchy]
   [renderer.shell.hierarchy :as shell.hierarchy]))

(hierarchy/derive! :cljs ::shell.hierarchy/language)

(defmethod shell.hierarchy/init :cljs
  []
  (print "Welcome to your ClojureScript REPL!")
  (print "")
  (print "You can create or modify shapes using the command line.")
  (print "Type `(help)` to see a list of commands.")
  (rf/dispatch [:renderer.shell.events/language-load-success]))

(defmethod shell.hierarchy/evaluate :cljs
  [_language s]
  s)
