(ns renderer.shell.impl.javascript
  (:require
   ["codemirror/mode/javascript/javascript.js"]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.hierarchy :as hierarchy]
   [renderer.shell.events :as-alias shell.events]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.reepl.replumb :as shell.utils.completion]
   [renderer.shell.subs :as-alias shell.subs]))

(hierarchy/derive! :js ::shell.hierarchy/language)

(defmethod shell.hierarchy/init :js
  [_language {:keys [on-success]}]
  ;; Expose all user functions to global namespace.
  (doseq [command (vals (ns-publics 'user))]
    (aset js/window (:name (meta command)) (.call ^js (.-val command))))

  (rf/dispatch on-success))

(defmethod shell.hierarchy/help :js
  [_language]
  (println "Type `help()` to see a list of commands."))

(defmethod shell.hierarchy/evaluate :js
  [_language s]
  (str "(js/eval \"" s "\")"))

(defmethod shell.hierarchy/codemirror-options :js
  [_language]
  {:mode "javascript"})

(defmethod shell.hierarchy/completion :js
  [_language s]
  (shell.utils.completion/js-completion s ""))

(rf/dispatch [::action.events/register-action
              {:id :shell-language/javascript
               :icon "javascript"
               :label [::label "JavaScript"]
               :event [::shell.events/activate-language :js]
               :active [::shell.subs/active-language? :js]}])
