(ns dev
  {:dev/always true}
  (:require
   [clojure.pprint :refer (pprint)]
   [clojure.string :as string]
   [malli.dev.cljs :as malli.dev]
   [re-frame.core :as rf]
   [renderer.app.events :as app.events]))

(comment
  ;; Full db validation
  (rf/reg-global-interceptor app.events/schema-validator)
  (rf/clear-global-interceptor ::app.events/schema-validator)

  ;; Enable function instrumentation
  ;; https://github.com/metosin/malli/blob/master/docs/clojurescript-function-instrumentation.md
  (malli.dev/start!)
  (malli.dev/stop!)

  (pprint (string/trim "This line suppresses some clj-kondo warnings.")))
