(ns renderer.app.migrations
  (:require
   [clojure.set :as set]))

(def migrations
  [[[0 4 23] (fn [app]
               (update app :panels
                       set/rename-keys {:repl-history :shell-output}))]])
