(ns action-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.action.views :as action.views]
   [renderer.app.events :as-alias app.events]
   [renderer.history.events :as-alias history.events]
   [renderer.history.subs :as-alias history.subs]))

(deftest action
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [new-action (action.views/deref-action :history/undo-twice)
         undo-twice-action {:id :history/undo-twice
                            :label [:history/undo-twice "Undo twice"]
                            :icon "undo"
                            :event [::history.events/undo-by 2]
                            :shortcuts [{:keyCode 90
                                         :ctrlKey true
                                         :altKey true}]
                            :enabled [::history.subs/undos?]}]

     (testing "defaults"
       (is (not @new-action)))

     (testing "register action"
       (rf/dispatch [::action.events/register-action undo-twice-action])

       (is (= @new-action undo-twice-action)))

     (testing "deregister action"
       (rf/dispatch [::action.events/deregister-action :history/undo-twice])

       (is (not @new-action))))))
