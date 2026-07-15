(ns renderer.shell.impl.clojurescript
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.hierarchy :as hierarchy]
   [renderer.shell.events :as-alias shell.events]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.subs :as-alias shell.subs]))

(hierarchy/derive! :cljs ::shell.hierarchy/language)

(defmethod shell.hierarchy/init :cljs
  [language {:keys [on-success]}]
  (rf/dispatch (conj on-success language)))

(defmethod shell.hierarchy/help :cljs
  [_language]
  (print "Global javascript objects and functions are accessible using the js"
         "namespace (e.g. `js/document`).")
  (print "Type `(help)` to see a list of commands."))

(defmethod shell.hierarchy/evaluate :cljs
  [_language s]
  s)

(defmethod shell.hierarchy/codemirror-mode :cljs
  [_language]
  "clojure")

(rf/dispatch [::action.events/register-action
              {:id :shell-language/clojurescript
               :label [::label "ClojureScript"]
               :event [::shell.events/activate-language :cljs]
               :active [::shell.subs/active-language? :cljs]}])
