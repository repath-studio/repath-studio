(ns history-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.events :as-alias element.events]
   [renderer.element.subs :as-alias element.subs]
   [renderer.history.events :as-alias history.events]
   [renderer.history.subs :as-alias history.subs]))

(deftest history
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [undos? (rf/subscribe [::history.subs/undos?])
         redos? (rf/subscribe [::history.subs/redos?])
         undos (rf/subscribe [::history.subs/undos])
         redos (rf/subscribe [::history.subs/redos])
         selected-elements (rf/subscribe [::element.subs/selected])
         active-document (rf/subscribe [::document.subs/active])
         saved? (rf/subscribe [::document.subs/active-saved?])]

     (testing "no undos/redos"
       (is (not @undos?))
       (is (not @redos?)))

     (testing "add action to history"
       (rf/dispatch [::element.events/add {:tag :rect
                                           :attrs {:x 100
                                                   :y 100
                                                   :width 100
                                                   :height 100}}])
       (is @undos?)
       (is (= (count @undos) 1))
       (is (not @redos?))
       (is (= (-> @selected-elements first :tag) :rect)))

     (rf/dispatch [::document.events/saved false {:id (:id @active-document)
                                                  :title "File-1"}])

     (testing "undo"
       (rf/dispatch [::history.events/undo])
       (is @redos?)
       (is (= (count @redos) 1))
       (is (not @undos?))
       (is (empty? @selected-elements))
       (is (not @saved?)))

     (testing "redo"
       (rf/dispatch [::history.events/redo])
       (is @undos?)
       (is (= (count @undos) 1))
       (is (not @redos?))
       (is (= (-> @selected-elements first :tag) :rect))
       (is @saved?))

     (testing "clear history"
       (rf/dispatch [::history.events/clear])
       (is (not @undos?))
       (is (not @redos?))
       (is @saved?)))))
