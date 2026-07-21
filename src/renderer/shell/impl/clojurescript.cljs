(ns renderer.shell.impl.clojurescript
  (:require
   ["codemirror/mode/clojure/clojure.js"]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.hierarchy :as hierarchy]
   [renderer.shell.events :as-alias shell.events]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.reepl.replumb :as shell.reepl.replumb]
   [renderer.shell.subs :as-alias shell.subs]))

(hierarchy/derive! :cljs ::shell.hierarchy/language)

(defmethod shell.hierarchy/init :cljs
  [{:keys [on-success]}]
  (rf/dispatch on-success))

(defmethod shell.hierarchy/help :cljs
  [_language]
  (println "Global javascript objects and functions are accessible using the js"
           "namespace (e.g. `js/document`).")
  (println "Type `(help)` to see a list of commands."))

(defmethod shell.hierarchy/evaluate :cljs
  [_language s]
  s)

(defmethod shell.hierarchy/codemirror-options :cljs
  [_language]
  {:mode "clojure"})

(defmethod shell.hierarchy/completion :cljs
  [_language s]
  (if (zero? (.indexOf s "js/"))
    (shell.reepl.replumb/js-completion (.slice s 3) "js/")
    (shell.reepl.replumb/cljs-completion s)))

(defmethod shell.hierarchy/docs :cljs
  [_language s]
  (when (symbol? s)
    (shell.reepl.replumb/process-doc s)))

(defmethod shell.hierarchy/show-error :cljs
  [_language v]
  (str "Error: " (:cause v)))

(rf/dispatch [::action.events/register-action
              {:id :shell-language/clojurescript
               :icon "clojurescript"
               :label [::label "ClojureScript"]
               :event [::shell.events/activate-language :cljs]
               :active [::shell.subs/active-language? :cljs]}])
