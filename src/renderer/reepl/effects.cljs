(ns renderer.reepl.effects
  (:require
   [re-frame.core :as rf]
   [renderer.reepl.replumb :as replumb]
   [replumb.repl :as replumb.repl]
   [shadow.cljs.bootstrap.browser :as bootstrap]))

(defn bootstrap-cb! []
  (replumb/run-repl "(in-ns 'user)" identity)
  (print "Welcome to your REPL!")
  (print "")
  (print "You can create or modify shapes using the command line.")
  (print "Type (help) to see a list of commands."))

(rf/reg-fx
 ::init
 (fn []
   ;; https://code.thheller.com/blog/shadow-cljs/2017/10/14/bootstrap-support.html
   (bootstrap/init replumb.repl/st {:path "js/bootstrap"
                                    :load-on-init '[user]} bootstrap-cb!)))
