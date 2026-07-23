(ns renderer.shell.impl.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.shell.impl.clojurescript]
   [renderer.shell.impl.javascript]
   [renderer.shell.impl.python]))

(rf/dispatch [::action.events/register-action-group
              {:id :shell/languages
               :label [::shell-languages "Shell languages"]
               :actions [:shell-language/clojurescript
                         :shell-language/javascript
                         :shell-language/python]}])
