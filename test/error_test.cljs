(ns error-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.error.events :as-alias error.events]
   [renderer.error.subs :as-alias error.subs]))

(deftest error-events
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [reporting? (rf/subscribe [::error.subs/reporting?])]
     (testing "initial state"
       (is (not @reporting?)))

     (testing "toggle reporting"
       (rf/dispatch [::error.events/toggle-reporting])
       (is @reporting?)))))
