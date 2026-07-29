(ns renderer.shell.impl.javascript
  (:require
   ["codemirror/mode/javascript/javascript.js"]
   [camel-snake-kebab.core :as camel-snake-kebab]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.hierarchy :as hierarchy]
   [renderer.shell.events :as-alias shell.events]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.reepl.replumb :as shell.utils.completion]
   [renderer.shell.subs :as-alias shell.subs]
   [user]))

(hierarchy/derive! :js ::shell.hierarchy/language)

(defn expose-command-to-global-namespace
  [command]
  (let [fn-val @command
        wrapper (fn [& args]
                  (apply fn-val (map #(js->clj % :keywordize-keys true)
                                     args)))]
    (aset js/window
          (camel-snake-kebab/->camelCaseString (:name (meta command)))
          wrapper)))

(defmethod shell.hierarchy/init :js
  [{:keys [on-success]}]
  ;; Expose all user functions to global namespace.
  (doseq [command (vals (ns-publics 'user))]
    (expose-command-to-global-namespace command))

  (rf/dispatch on-success))

(defmethod shell.hierarchy/help :js
  [_language command]
  (if-let [f (get (ns-publics 'user) (symbol command))]
    (print (camel-snake-kebab/->camelCaseString (:name (meta f)))
           " - "
           (:doc (meta f)))
    (println "Command not found:" command)))

(defmethod shell.hierarchy/welcome :js
  [_language]
  (println "Type `help()` to see a list of commands."))

(defmethod shell.hierarchy/evaluate :js
  [_language s]
  (str "(js/eval \""
       (-> s
           (string/replace "\\" "\\\\")
           (string/replace "\"" "\\\""))
       "\")"))

(defmethod shell.hierarchy/codemirror-options :js
  [_language]
  {:mode "javascript"})

(defmethod shell.hierarchy/completions :js
  [_language s]
  (shell.utils.completion/js-completion s ""))

(defmethod shell.hierarchy/show-error :js
  [_language v]
  (str (when-let [error-type (:type (last (:via v)))]
         (str (name (keyword error-type)) ": "))
       (:cause v)))

(rf/dispatch [::action.events/register-action
              {:id :shell-language/javascript
               :icon "javascript"
               :label [::label "JavaScript"]
               :event [::shell.events/activate-language :js]
               :active [::shell.subs/active-language? :js]}])
