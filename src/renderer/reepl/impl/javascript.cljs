(ns renderer.reepl.impl.javascript
  (:require
   [re-frame.core :as rf]
   [renderer.hierarchy :as hierarchy]
   [renderer.reepl.hierarchy :as reepl.hierarchy]))

(hierarchy/derive! :js ::reepl.hierarchy/language)

(defmethod reepl.hierarchy/init :js
  []
  (doseq [command (vals (ns-publics 'user))]
    (aset js/window (:name (meta command)) (.call ^js (.-val command))))

  (print "Welcome to your Javascript REPL!")
  (print "")
  (print "You can create or modify shapes using the command line.")
  (print "Type `help()` to see a list of commands.")
  (rf/dispatch [:renderer.reepl.events/language-load-success]))

(defmethod reepl.hierarchy/evaluate :js
  [_language s]
  (str "(js/eval \"" s "\")"))
