(ns renderer.shell.effects
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.reepl.replumb :as shell.reepl.replumb]
   [renderer.utils.dom :as utils.dom]
   [replumb.repl :as replumb.repl]
   [shadow.cljs.bootstrap.browser :as bootstrap]))

(defn print-fn
  [log]
  (fn [& args]
    (if (= 1 (count args))
      (log (first args))
      (log args))))

(defn set-print!
  [log]
  (set! cljs.core/*print-newline* false)
  (set-print-err-fn! (print-fn log))
  (set-print-fn! (print-fn log)))

(rf/reg-fx
 ::init
 (fn [[event params]]
   (set-print! #(rf/dispatch (conj event :output %)))

   ;; https://code.thheller.com/blog/shadow-cljs/2017/10/14/bootstrap-support.html
   (bootstrap/init replumb.repl/st
                   {:path "js/bootstrap"
                    :load-on-init '[user]}
                   #(shell.reepl.replumb/run-repl "(in-ns 'user)"
                                                  (fn []
                                                    (shell.hierarchy/init
                                                     params))))))

(rf/reg-fx
 ::init-language
 (fn [params]
   (shell.hierarchy/init params)))

(rf/reg-fx
 ::welcome
 (fn [language]
   (println "Welcome to your " (string/upper-case (name language)) " shell!")
   (println "You can create or modify shapes using the command line.")
   (println)
   (shell.hierarchy/welcome language)))

(rf/reg-fx
 ::focus
 (fn []
   (some-> (.getElementById js/document utils.dom/shell-input-id)
           (.getElementsByTagName "textarea")
           (first)
           (.focus))))

(rf/reg-fx
 ::execute
 (fn [{:keys [text language verbose callback-event]}]
   (try (shell.reepl.replumb/run-repl (shell.hierarchy/evaluate language text)
                                      {:verbose verbose}
                                      (fn [item-type result]
                                        (rf/dispatch (conj callback-event
                                                           item-type
                                                           result))))
        (catch :default e (rf/dispatch (->> (cljs.core/Throwable->map e)
                                            (conj callback-event :error)))))))
