(ns tool-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.element.events :as-alias element.events]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]))

(deftest tool
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [active-tool (rf/subscribe [::tool.subs/active])]

     (testing "initial"
       (is (= @active-tool :transform)))

     (testing "edit tool"
       (rf/dispatch [::tool.events/activate :edit])
       (is (= @active-tool :edit))
       (is (= (tool.hierarchy/render :edit) [:g]))

       (rf/dispatch [::element.events/add {:tag :rect
                                           :attrs {:width 100
                                                   :height 100}}])

       (rf/dispatch [::tool.events/activate :edit])
       (is (not= (tool.hierarchy/render :edit) [:g])))

     (testing "cancel"
       (rf/dispatch [::tool.events/cancel])
       (is (= @active-tool :transform))))))
