(ns icon-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.icon.defaults :as icon.defaults]
   [renderer.icon.events :as-alias icon.events]
   [renderer.icon.subs :as-alias icon.subs]))

(deftest icon
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [file-icon-path (rf/subscribe [::icon.subs/path-data "file"])
         registered-icon-path (rf/subscribe [::icon.subs/path-data "new-icon"])
         new-icon {:id "new-icon"
                   :path "M10 10 H 90 V 90 H 10 Z"}]

     (testing "defaults"
       (is (= @file-icon-path (get-in icon.defaults/icons ["file" :path])))
       (is (not @registered-icon-path)))

     (testing "register"
       (rf/dispatch [::icon.events/register-icon new-icon])

       (is (= @registered-icon-path (:path new-icon))))

     (testing "deregister"
       (rf/dispatch [::icon.events/deregister-icon "new-icon"])

       (is (not @registered-icon-path))))))
