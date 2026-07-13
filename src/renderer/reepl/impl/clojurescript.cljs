(ns renderer.reepl.impl.clojurescript
  (:require
   [re-frame.core :as rf]
   [renderer.reepl.hierarchy :as reepl.hierarchy]))

(defmethod reepl.hierarchy/init :cljs
  []
  (print "Welcome to your ClojureScript REPL!")
  (print "")
  (print "You can create or modify shapes using the command line.")
  (print "Type `(help)` to see a list of commands.")
  (rf/dispatch [:renderer.reepl.events/language-loaded]))

