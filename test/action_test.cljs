(ns action-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.action.defaults :as action.defaults]
   [renderer.action.events :as-alias action.events]
   [renderer.action.subs :as-alias action.subs]
   [renderer.app.events :as-alias app.events]
   [renderer.history.events :as-alias history.events]
   [renderer.history.subs :as-alias history.subs]
   [renderer.utils.key :as utils.key]))

(deftest action
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [existing-action (rf/subscribe [::action.subs/entity :document/new])
         new-action (rf/subscribe [::action.subs/entity :history/undo-twice])
         undo-twice-action {:id :history/undo-twice
                            :label [::undo "Undo twice"]
                            :icon "undo"
                            :event [::history.events/undo-by 2]
                            :shortcuts [{:keyCode (utils.key/codes "Z")
                                         :ctrlKey true
                                         :altKey true}]
                            :enabled [::history.subs/undos?]}]

     (testing "defaults"
       (is (= @existing-action (get action.defaults/registry :document/new)))
       (is (not @new-action)))

     (testing "register"
       (rf/dispatch [::action.events/register-action undo-twice-action])

       (is (= @new-action undo-twice-action)))

     (testing "deregister"
       (rf/dispatch [::action.events/deregister-action :history/undo-twice])

       (is (not @new-action))))))
