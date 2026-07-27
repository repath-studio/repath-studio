(ns dialog-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.dialog.events :as-alias dialog.events]
   [renderer.dialog.subs :as-alias dialog.subs]))

(deftest dialog-events
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [active-dialog (rf/subscribe [::dialog.subs/active])]
     (testing "show about dialog"
       (rf/dispatch [::dialog.events/show-about])
       (is (= (:title @active-dialog) [:div.sr-only "Repath Studio"])))

     (testing "close dialog"
       (rf/dispatch [::dialog.events/close])
       (is (nil? @active-dialog))))))
