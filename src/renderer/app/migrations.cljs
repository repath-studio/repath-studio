(ns renderer.app.migrations
  (:require
   [clojure.set :as set]))

(def migrations
  [[[0 4 23] (fn [app]
               (update app :panels
                       update-vals
                       #(set/rename-keys % {:repl-history :shell-output})))]])
