(ns renderer.reepl.impl.javascript
  (:require
   [re-frame.core :as rf]
   [renderer.reepl.hierarchy :as reepl.hierarchy]))

(defmethod reepl.hierarchy/init :js
  []
  (print "Welcome to your Javascript REPL!")
  (print "")
  (print "You can create or modify shapes using the command line.")
  (print "Type `help()` to see a list of commands.")
  (rf/dispatch [:renderer.reepl.events/language-loaded]))
