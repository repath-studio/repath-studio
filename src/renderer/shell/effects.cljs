(ns renderer.shell.effects
  (:require
   [re-frame.core :as rf]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.reepl.replumb :as shell.reepl.replumb]
   [renderer.utils.dom :as utils.dom]
   [replumb.repl :as replumb.repl]
   [shadow.cljs.bootstrap.browser :as bootstrap]))

(defn set-print!
  [log]
  (set! cljs.core/*print-newline* false)
  (set-print-err-fn!
   (fn [& args]
     (if (= 1 (count args))
       (log (first args))
       (log args))))
  (set-print-fn!
   (fn [& args]
     (if (= 1 (count args))
       (log (first args))
       (log args)))))

(rf/reg-fx
 ::init
 (fn [event]
   (set-print! #(rf/dispatch (conj event :output %)))

   ;; https://code.thheller.com/blog/shadow-cljs/2017/10/14/bootstrap-support.html
   (bootstrap/init replumb.repl/st
                   {:path "js/bootstrap"
                    :load-on-init '[user]}
                   #(shell.reepl.replumb/run-repl "(in-ns 'user)" nil))))

(rf/reg-fx
 ::init-language
 (fn [[language params]]
   (shell.hierarchy/init language params)))

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

(rf/reg-fx
 ::execute
 (fn [{:keys [text language verbose event]}]
   (shell.reepl.replumb/run-repl (shell.hierarchy/evaluate language text)
                                 {:verbose verbose}
                                 #(rf/dispatch (conj event %1 %2)))))
