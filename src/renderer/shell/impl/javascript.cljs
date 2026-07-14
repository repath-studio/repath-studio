(ns renderer.shell.impl.javascript
  (:require
   [re-frame.core :as rf]
   [renderer.hierarchy :as hierarchy]
   [renderer.shell.hierarchy :as shell.hierarchy]))

(hierarchy/derive! :js ::shell.hierarchy/language)

(defmethod shell.hierarchy/init :js
  []
  (doseq [command (vals (ns-publics 'user))]
    (aset js/window (:name (meta command)) (.call ^js (.-val command))))

  (print "Welcome to your Javascript REPL!")
  (print "")
  (print "You can create or modify shapes using the command line.")
  (print "Type `help()` to see a list of commands.")
  (rf/dispatch [:renderer.shell.events/language-load-success]))

(defmethod shell.hierarchy/evaluate :js
  [_language s]
  (str "(js/eval \"" s "\")"))
