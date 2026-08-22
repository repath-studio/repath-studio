(ns timeline-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.timeline.events :as-alias timeline.events]
   [renderer.timeline.subs :as-alias timeline.subs]))

(deftest timeline-events
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [paused? (rf/subscribe [::timeline.subs/paused?])
         grid-snap? (rf/subscribe [::timeline.subs/grid-snap?])
         guide-snap? (rf/subscribe [::timeline.subs/guide-snap?])
         replay? (rf/subscribe [::timeline.subs/replay?])
         speed (rf/subscribe [::timeline.subs/speed])
         t (rf/subscribe [::timeline.subs/time])]
     (testing "initial state"
       (is (true? @paused?))
       (is (true? @grid-snap?))
       (is (true? @guide-snap?))
       (is (false? @replay?))
       (is (= @speed 1))
       (is (= @t 0)))

     (testing "set grid snap"
       (rf/dispatch [::timeline.events/toggle-grid-snap])
       (is (false? @grid-snap?)))

     (testing "set guide snap"
       (rf/dispatch [::timeline.events/toggle-guide-snap])
       (is (false? @guide-snap?)))

     (testing "toggle replay"
       (rf/dispatch [::timeline.events/toggle-replay])
       (is (true? @replay?)))

     (testing "set speed"
       (rf/dispatch [::timeline.events/set-speed 2])
       (is (= @speed 2))))))
