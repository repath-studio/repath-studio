(ns panel-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.panel.events :as-alias panel.events]
   [renderer.panel.subs :as-alias panel.subs]))

(deftest panel-events
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [tree (rf/subscribe [::panel.subs/visible? :tree])
         attributes (rf/subscribe [::panel.subs/visible? :attributes])
         xml (rf/subscribe [::panel.subs/visible? :xml])
         history (rf/subscribe [::panel.subs/visible? :history])
         repl-history (rf/subscribe [::panel.subs/visible? :repl-history])
         timeline (rf/subscribe [::panel.subs/visible? :timeline])]
     (testing "initial state"
       (is (true? @tree))
       (is (true? @attributes))
       (is (false? @xml))
       (is (false? @history))
       (is (false? @repl-history))
       (is (false? @timeline)))

     (testing "toggle panel"
       (rf/dispatch [::panel.events/toggle :tree])
       (is (false? @tree))))))
