(ns action-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.action.subs :as-alias action.subs]
   [renderer.app.events :as-alias app.events]
   [renderer.history.events :as-alias history.events]
   [renderer.history.subs :as-alias history.subs]))

(deftest action
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [new-action (rf/subscribe [::action.subs/action :history/undo-twice])
         new-group (rf/subscribe [::action.subs/action-group
                                  :history/undo-group])
         undo-twice-action {:id :history/undo-twice
                            :label [:history/undo-twice "Undo twice"]
                            :icon "undo"
                            :event [::history.events/undo-by 2]
                            :shortcuts [{:keyCode 90
                                         :ctrlKey true
                                         :altKey true}]
                            :enabled [::history.subs/undos?]}
         undo-group {:id :history/undo-group
                     :icon "history"
                     :label [:history/undo-group "Undo group"]
                     :actions [:history/undo-twice]}]

     (testing "defaults"
       (is (not @new-action))
       (is (not @new-group)))

     (testing "register action"
       (rf/dispatch [::action.events/register-action undo-twice-action])

       (is (= @new-action undo-twice-action)))

     (testing "deregister action"
       (rf/dispatch [::action.events/deregister-action :history/undo-twice])

       (is (not @new-action)))

     (testing "register action group"
       (rf/dispatch [::action.events/register-action-group undo-group])

       (is (= @new-group undo-group)))

     (testing "deregister action group"
       (rf/dispatch [::action.events/deregister-action-group
                     :history/undo-group])

       (is (not @new-group))))))

(deftest shortcuts
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [default-shortcut {:keyCode 90
                           :ctrlKey true
                           :altKey true}
         extra-shortcut {:keyCode 89
                         :ctrlKey true}
         undo-action {:id :history/undo-twice
                      :label [:history/undo-twice "Undo twice"]
                      :icon "undo"
                      :event [::history.events/undo-by 2]
                      :shortcuts [default-shortcut]}
         effective (rf/subscribe [::action.subs/action-shortcuts
                                  :history/undo-twice])]

     (rf/dispatch [::action.events/register-action undo-action])

     (testing "falls back to default shortcuts"
       (is (= @effective #{default-shortcut})))

     (testing "add shortcut keeps defaults"
       (rf/dispatch [::action.events/add-shortcut
                     :history/undo-twice extra-shortcut])
       (is (= @effective #{default-shortcut extra-shortcut})))

     (testing "remove shortcut overrides defaults"
       (rf/dispatch [::action.events/remove-shortcut
                     :history/undo-twice default-shortcut])
       (is (= @effective #{extra-shortcut})))

     (testing "removing all shortcuts overrides with an empty set"
       (rf/dispatch [::action.events/remove-shortcut
                     :history/undo-twice extra-shortcut])
       (is (= @effective #{})))

     (testing "reset restores default shortcuts"
       (rf/dispatch [::action.events/reset-shortcuts :history/undo-twice])
       (is (= @effective #{default-shortcut}))))))
