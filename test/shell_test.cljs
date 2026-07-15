(ns shell-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.shell.events :as-alias shell.events]
   [renderer.shell.subs :as-alias shell.subs]))

(deftest shell-language-switching
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [active-language (rf/subscribe [::shell.subs/active-language])]

     (testing "initial"
       (is (= @active-language :cljs)))

     (testing "activate javascript"
       (rf/dispatch [::shell.events/activate-language :js])

       (is (= @active-language :js))))))

