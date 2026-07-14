(ns renderer.shell.effects
  (:require
   [re-frame.core :as rf]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.reepl.replumb :as replumb]
   [renderer.utils.dom :as utils.dom]
   [replumb.repl :as replumb.repl]
   [shadow.cljs.bootstrap.browser :as bootstrap]))

(rf/reg-fx
 ::init
 (fn []
   ;; https://code.thheller.com/blog/shadow-cljs/2017/10/14/bootstrap-support.html
   (bootstrap/init replumb.repl/st
                   {:path "js/bootstrap"
                    :load-on-init '[user]}
                   #(replumb/run-repl "(in-ns 'user)" identity))))

(rf/reg-fx
 ::init-language
 (fn [language]
   (shell.hierarchy/init language)))

(rf/reg-fx
 ::welcome
 (fn [language]
   (print "Welcome to your " (name language) " REPL!")
   (print "You can create or modify shapes using the command line.")
   (print "")
   (shell.hierarchy/help language)))

(rf/reg-fx
 ::focus
 (fn []
   (some-> (.getElementById js/document utils.dom/shell-input-id)
           (.getElementsByTagName "textarea")
           (first)
           (.focus))))
