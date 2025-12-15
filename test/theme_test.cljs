(ns theme-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.theme.events :as-alias theme.events]
   [renderer.theme.subs :as-alias theme.subs]))

(deftest mode
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [theme-mode (rf/subscribe [::theme.subs/mode])
         computed-mode (rf/subscribe [::theme.subs/computed-mode])]

     (testing "default theme"
       (is (= :dark @theme-mode))
       (is (= :dark @computed-mode)))

     (testing "theme cycling"
       (rf/dispatch [::theme.events/set-mode :light])
       (is (= :light @theme-mode))
       (is (= :light @computed-mode))

       (rf/dispatch [::theme.events/set-mode :dark])
       (is (= :dark @theme-mode))
       (is (= :dark @computed-mode))

       (rf/dispatch [::theme.events/set-mode :system])
       (is (= :system @theme-mode))
       (is (= :light @computed-mode))))))
