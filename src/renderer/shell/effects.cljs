(ns renderer.shell.effects
  (:require
   ["@codemirror/view" :refer [EditorView]]
   [clojure.string :as string]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.reepl.sci :as shell.reepl.sci]
   [renderer.utils.codemirror :as utils.codemirror]
   [renderer.utils.dom :as utils.dom]
   [renderer.utils.extra :refer [log]]))

(rf/reg-fx
 ::init
 (fn [[event params]]
   (shell.reepl.sci/set-print! #(rf/dispatch (conj event :info %)))
   (shell.reepl.sci/init! (fn [error]
                            (if error
                              (rf/dispatch (conj (get params :on-error) error))
                              (shell.hierarchy/init params))))))

(rf/reg-fx
 ::focus
 (fn []
   (some-> (utils.dom/get-shell-element)
           (.focus))))

(rf/reg-fx
 ::init-language
 (fn [params]
   (shell.hierarchy/init params)))

(rf/reg-fx
 ::welcome
 (fn [language]
   (log "Welcome to your " (string/upper-case (name language)) " shell! "
        "You can create or modify shapes using the command line.")
   (log "See " [:url "https://repath.studio/get-started/interactive-shell/"]
        " for examples.")
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

(defn get-editor-from-dom
  []
  (some->> (utils.dom/get-shell-element)
           (.findFromDOM EditorView)))

(rf/reg-fx
 ::replace-current-word
 (fn [s]
   (when-let [inst (get-editor-from-dom)]
     (when-let [current-word (utils.codemirror/current-word inst)]
       (.dispatch inst #js {:changes #js {:from (.-from current-word)
                                          :to (.-to current-word)
                                          :insert s}})))))

(rf/reg-fx
 ::complete-word
 (fn [[lang {:keys [on-success on-error]}]]
   (when-let [inst (get-editor-from-dom)]
     (if-let [result (utils.codemirror/current-word inst)]
       (let [from (.-from result)
             to (.-to result)
             text (.sliceDoc (.-state inst) from to)
             words (when-not (empty? text)
                     (->> (shell.hierarchy/completions lang text)
                          (take config/max-shell-completions)
                          (into [])))]
         (rf/dispatch (conj on-success text words)))
       (rf/dispatch on-error)))))

