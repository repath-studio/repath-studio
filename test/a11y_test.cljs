(ns a11y-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.a11y.events :as-alias a11y.events]
   [renderer.a11y.subs :as-alias a11y.subs]
   [renderer.app.events :as-alias app.events]))

(deftest filters
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [a11y-filters (rf/subscribe [::a11y.subs/filters])]

     (testing "defaults"
       (is (= (count @a11y-filters) 10)))

     (testing "register"
       (rf/dispatch [::a11y.events/register-filter
                     {:id :blur-x3
                      :tag :feGaussianBlur
                      :label [:a11y-filter/blur-x3 "blur-x3"]
                      :attrs {:in "SourceGraphic"
                              :type "matrix"
                              :stdDeviation "3"}}])

       (is (= (count @a11y-filters) 11)))

     (testing "deregister"
       (rf/dispatch [::a11y.events/deregister-filter :blur-x3])

       (is (= (count @a11y-filters) 10)))

     (testing "active filter"
       (let [active-filter (rf/subscribe [::a11y.subs/active-filter])]
         (testing "default state"
           (is (not @active-filter)))

         (testing "enable filter"
           (rf/dispatch [::a11y.events/toggle-active-filter :blur])
           (is (= @active-filter :blur)))

         (testing "change active filter"
           (rf/dispatch [::a11y.events/toggle-active-filter :deuteranopia])
           (is (= @active-filter :deuteranopia)))

         (testing "disable filter"
           (rf/dispatch [::a11y.events/toggle-active-filter :deuteranopia])
           (is (not @active-filter))))))))
