(ns renderer.shell.effects
  (:require
   ["@codemirror/view" :refer [EditorView]]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.reepl.sci :as shell.reepl.sci]
   [renderer.utils.codemirror :as utils.codemirror]
   [renderer.utils.dom :as utils.dom]))

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
   (shell.reepl.sci/init! (fn [error]
                            (if error
                              (rf/dispatch (conj (get params :on-error) error))
                              (shell.hierarchy/init params))))))

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
 ::execute
 (fn [{:keys [text language verbose callback-event]}]
   (try (shell.reepl.sci/execute (shell.hierarchy/evaluate language text)
                                 verbose
                                 (fn [item-type result]
                                   (rf/dispatch (conj callback-event
                                                      item-type
                                                      result))))
        (catch :default e (rf/dispatch (->> (cljs.core/Throwable->map e)
                                            (conj callback-event :error)))))))

(rf/reg-fx
 ::replace-current-word
 (fn [s]
   (when-let [inst (some-> (utils.dom/get-shell-element)
                           (EditorView.findFromDOM))]
     (let [current-word (utils.codemirror/current-word inst)]
       (.dispatch inst #js {:changes #js {:from (.-from current-word)
                                          :to (.-to current-word)
                                          :insert s}})))))
