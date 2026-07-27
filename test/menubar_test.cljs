(ns menubar-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.menubar.events :as-alias menubar.events]
   [renderer.menubar.subs :as-alias menubar.subs]))

(deftest menubar-events
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [indicator? (rf/subscribe [::menubar.subs/indicator?])
         active-menu (rf/subscribe [::menubar.subs/active-menu])]
     (testing "initial state"
       (is (not @indicator?))
       (is (not @active-menu)))

     (testing "activate file"
       (rf/dispatch [::menubar.events/activate :file])
       (is (not @indicator?))
       (is (= @active-menu :file)))

     (testing "deactivate"
       (rf/dispatch [::menubar.events/deactivate])
       (is (not @indicator?))
       (is (nil? @active-menu))))))

