(ns frame-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.frame.events :as-alias frame.events]
   [renderer.frame.subs :as-alias frame.subs]))

(deftest frame
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [viewbox (rf/subscribe [::frame.subs/viewbox])
         zoom (rf/subscribe [::document.subs/zoom])
         pan (rf/subscribe [::document.subs/pan])
         viewbox-attr (rf/subscribe [::frame.subs/viewbox-attr])]

     (testing "resize"
       (rf/dispatch [::frame.events/resize {:x 252
                                            :y 139
                                            :width 1946
                                            :height 945
                                            :top 139
                                            :right 2198
                                            :bottom 1084
                                            :left 252}])

       (is (= @viewbox [0 0 1946 945]))
       (is (= @viewbox-attr "0 0 1946 945")))

     (testing "zoom"
       (is (= @zoom 1))

       (rf/dispatch [::frame.events/zoom-in])
       (is (> @zoom 1))

       (rf/dispatch [::frame.events/zoom-out])
       (is (= @zoom 1))

       (rf/dispatch [::frame.events/zoom-out])
       (is (< @zoom 1))

       (rf/dispatch [::frame.events/set-zoom 1])
       (is (= @zoom 1)))

     (testing "focus"
       (is (= @zoom 1))

       (rf/dispatch [::frame.events/focus-selection :original])
       (is (= @viewbox [-675.5 -51.5 1946 945]))
       (is (= @zoom 1))
       (is (= @pan [-675.5 -51.5]))

       (rf/dispatch [::frame.events/focus-selection :fit])
       (is (> @zoom 1))))))
