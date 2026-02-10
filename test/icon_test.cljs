(ns icon-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.icon.defaults :as icon.defaults]
   [renderer.icon.events :as-alias icon.events]
   [renderer.icon.subs :as-alias icon.subs]))

(deftest filters
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [file-icon (rf/subscribe [::icon.subs/icon "file"])
         registered-icon (rf/subscribe [::icon.subs/icon "new-icon"])
         new-icon {:id "extra"
                   :path "M10 10 H 90 V 90 H 10 Z"}]

     (testing "defaults"
       (is (= @file-icon (get icon.defaults/icons "new-icon")))
       (is (not @registered-icon)))

     (testing "register"
       (rf/dispatch [::icon.events/register-icon new-icon])

       (is (= @registered-icon new-icon)))

     (testing "deregister"
       (rf/dispatch [::icon.events/deregister-icon "extra"])

       (is (not @registered-icon))))))
