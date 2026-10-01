(ns renderer.app.migrations
  (:require
   [clojure.set :as set]))

(def migrations
  [[[0 4 23] (fn [app]
               (-> app
                   (update :snap dissoc :transient-active :threshold)
                   (update :panels set/rename-keys
                           {:repl-history :shell-output})))]])
