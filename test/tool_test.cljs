(ns tool-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.element.events :as-alias element.events]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.edit.core :as-alias tool.impl.base.edit]
   [renderer.tool.impl.base.transform.core :as-alias tool.impl.base.transform]
   [renderer.tool.subs :as-alias tool.subs]))

(deftest tool
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [active-tool (rf/subscribe [::tool.subs/active])]

     (testing "initial"
       (is (= @active-tool ::tool.impl.base.transform/transform)))

     (testing "edit tool"
       (rf/dispatch [::tool.events/activate ::tool.impl.base.edit/edit])
       (is (= @active-tool ::tool.impl.base.edit/edit))
       (is (= (tool.hierarchy/render ::tool.impl.base.edit/edit) [:g]))

       (rf/dispatch [::element.events/add {:tag :rect
                                           :attrs {:width 100
                                                   :height 100}}])

       (rf/dispatch [::tool.events/activate ::tool.impl.base.edit/edit])
       (is (not= (tool.hierarchy/render ::tool.impl.base.edit/edit) [:g])))

     (testing "cancel"
       (rf/dispatch [::tool.events/cancel])
       (is (= @active-tool ::tool.impl.base.transform/transform))))))
