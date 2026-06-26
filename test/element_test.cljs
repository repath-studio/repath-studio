(ns element-test
  (:require
   [cljs.test :refer-macros [deftest are is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.document.events :as-alias document.events]
   [renderer.element.db :as element.db]
   [renderer.element.events :as-alias element.events]
   [renderer.element.subs :as-alias element.subs]))

(deftest init
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [root (rf/subscribe [::element.subs/root])
         root-children (rf/subscribe [::element.subs/root-children])]
     (testing "default elements"
       (is (element.db/valid? @root))
       (are [v sub] (= v sub)
         :canvas (:tag @root)
         :svg (-> @root-children first :tag))))))

(deftest select
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [selected (rf/subscribe [::element.subs/selected])]
     (testing "default state"
       (is (empty? @selected)))

     (testing "select all"
       (rf/dispatch [::element.events/select-all])
       (is (= (count @selected) 1)))

     (testing "deselect all"
       (rf/dispatch [::element.events/deselect-all])
       (is (empty? @selected)))

     (testing "select same tags"
       (rf/dispatch [::element.events/add {:tag :rect
                                           :attrs {:width "100"
                                                   :height "100"}}])
       (is (= :rect (-> @selected first :tag)))

       (rf/dispatch [::element.events/add {:tag :rect
                                           :attrs {:width "100"
                                                   :height "100"}}])
       (rf/dispatch [::element.events/select-same-tags])
       (is (= (count @selected) 2))))))

(deftest index
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])
   (rf/dispatch [::document.events/new-from-template nil])

   (rf/dispatch [::element.events/add {:tag :rect
                                       :attrs {:width "100"
                                               :height "100"}}])
   (rf/dispatch [::element.events/add {:tag :circle
                                       :attrs {:r "100"
                                               :cx "100"
                                               :cy "100"}}])

   (rf/dispatch [::element.events/add {:tag :line
                                       :attrs {:x1 "0"
                                               :y1 "0"
                                               :x2 "50"
                                               :y2 "50"}}])

   (let [elements (rf/subscribe [::element.subs/root-children])]
     (testing "initial order"
       (is (= (mapv :tag @elements) [:rect :circle :line])))

     (testing "lower"
       (rf/dispatch [::element.events/lower])
       (is (= (mapv :tag @elements) [:rect :line :circle])))

     (testing "raise"
       (rf/dispatch [::element.events/raise])
       (is (= (mapv :tag @elements) [:rect :circle :line])))

     (testing "raise when already on top"
       (rf/dispatch [::element.events/raise])
       (is (= (mapv :tag @elements) [:rect :circle :line])))

     (testing "lower to bottom"
       (rf/dispatch [::element.events/lower-to-bottom])
       (is (= (mapv :tag @elements) [:line :rect :circle])))

     (testing "raise to top"
       (rf/dispatch [::element.events/raise-to-top])
       (is (= (mapv :tag @elements) [:rect :circle :line]))))))

(deftest align
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])
   (rf/dispatch [::document.events/new-from-template [800 600]])

   (rf/dispatch [::element.events/add {:tag :rect
                                       :attrs {:x "50"
                                               :y "50"
                                               :width "100"
                                               :height "100"}}])

   (let [elements (rf/subscribe [::element.subs/selected])]
     (testing "align left"
       (rf/dispatch [::element.events/align :left])
       (is (= (-> @elements first :attrs) {:x "0"
                                           :y "50"
                                           :width "100"
                                           :height "100"})))

     (testing "align top"
       (rf/dispatch [::element.events/align :top])
       (is (= (-> @elements first :attrs) {:x "0"
                                           :y "0"
                                           :width "100"
                                           :height "100"})))

     (testing "align right"
       (rf/dispatch [::element.events/align :right])
       (is (= (-> @elements first :attrs) {:x "700"
                                           :y "0"
                                           :width "100"
                                           :height "100"})))

     (testing "align bottom"
       (rf/dispatch [::element.events/align :bottom])
       (is (= (-> @elements first :attrs) {:x "700"
                                           :y "500"
                                           :width "100"
                                           :height "100"})))

     (testing "align center vertical"
       (rf/dispatch [::element.events/align :center-vertical])
       (is (= (-> @elements first :attrs) {:x "700"
                                           :y "250"
                                           :width "100"
                                           :height "100"})))

     (testing "align center horizontal"
       (rf/dispatch [::element.events/align :center-horizontal])
       (is (= (-> @elements first :attrs) {:x "350"
                                           :y "250"
                                           :width "100"
                                           :height "100"}))))))

(deftest visibility
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [selected (rf/subscribe [::element.subs/selected])]
     (rf/dispatch [::element.events/add {:tag :rect
                                         :attrs {:width "100"
                                                 :height "100"}}])
     (testing "default state"
       (is (-> @selected first :visible)))

     (testing "toggle visibility"
       (rf/dispatch [::element.events/toggle-prop (-> @selected first :id)
                     :visible])
       (is (not (-> @selected first :visible))))

     (testing "revert visibility"
       (rf/dispatch [::element.events/toggle-prop (-> @selected first :id)
                     :visible])
       (is (-> @selected first :visible))))))

(deftest label
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [selected (rf/subscribe [::element.subs/selected])]
     (rf/dispatch [::element.events/add {:tag :rect
                                         :attrs {:width "100"
                                                 :height "100"}}])
     (testing "default state"
       (is (not (-> @selected first :label))))

     (testing "set label"
       (rf/dispatch [::element.events/set-label (-> @selected first :id)
                     "rect"])
       (is (= (-> @selected first :label) "rect")))

     (testing "clear label"
       (rf/dispatch [::element.events/set-label (-> @selected first :id)
                     ""])
       (is (not (-> @selected first :label)))))))

(deftest lock
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [selected (rf/subscribe [::element.subs/selected])]
     (rf/dispatch [::element.events/add {:tag :rect
                                         :attrs {:width "100"
                                                 :height "100"}}])
     (testing "default state"
       (is (not (-> @selected first :locked))))

     (testing "lock"
       (rf/dispatch [::element.events/lock])
       (is (-> @selected first :locked)))

     (testing "unlock"
       (rf/dispatch [::element.events/unlock])
       (is (not (-> @selected first :locked)))))))

(deftest attribute
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [selected (rf/subscribe [::element.subs/selected])]
     (rf/dispatch [::element.events/add {:tag :rect
                                         :attrs {:width "100"
                                                 :height "100"
                                                 :fill "white"}}])
     (is (= (-> @selected first :attrs :fill) "white"))

     (testing "set attribute"
       (rf/dispatch [::element.events/set-attr :fill "red"])
       (is (= (-> @selected first :attrs :fill) "red")))

     (testing "preview attribute"
       (rf/dispatch [::element.events/preview-attr :fill "yellow"])
       (is (= (-> @selected first :attrs :fill) "yellow")))

     (testing "remove attribute"
       (rf/dispatch [::element.events/remove-attr :fill])
       (is (not (-> @selected first :attrs :fill)))))))

(deftest delete
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [selected (rf/subscribe [::element.subs/selected])]
     (rf/dispatch [::element.events/add {:tag :rect
                                         :attrs {:width "100"
                                                 :height "100"}}])

     (testing "delete selection"
       (let [id (-> @selected first :id)]
         (is (uuid? id))
         (rf/dispatch [::element.events/delete])
         (is (empty? @selected))
         (is (not @(rf/subscribe [::element.subs/entity id]))))))))

(deftest scale
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [selected (rf/subscribe [::element.subs/selected])]
     (rf/dispatch [::element.events/add {:tag :rect
                                         :attrs {:width "100"
                                                 :height "100"}}])
     (rf/dispatch [::element.events/scale [2 4]])
     (is (= (-> @selected first :attrs :width) "200"))
     (is (= (-> @selected first :attrs :height) "400")))))

(deftest translate
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [selected (rf/subscribe [::element.subs/selected])]
     (rf/dispatch [::element.events/add {:tag :rect
                                         :attrs {:x "100"
                                                 :y "100"
                                                 :width "100"
                                                 :height "100"}}])
     (rf/dispatch [::element.events/translate [50 100]])
     (is (= (-> @selected first :attrs :x) "150"))
     (is (= (-> @selected first :attrs :y) "200")))))

(deftest place
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [selected (rf/subscribe [::element.subs/selected])]
     (rf/dispatch [::element.events/add {:tag :rect
                                         :attrs {:x "100"
                                                 :y "100"
                                                 :width "100"
                                                 :height "100"}}])
     (rf/dispatch [::element.events/place [100 100]])
     (is (= (-> @selected first :attrs :x) "50"))
     (is (= (-> @selected first :attrs :y) "50")))))

(deftest ->path
  (rf.test/run-test-async
   (rf/dispatch-sync [::app.events/initialize])

   (rf.test/wait-for
    [::app.events/set-loading false]

    (let [selected (rf/subscribe [::element.subs/selected])]
      (rf/dispatch [::element.events/add {:tag :rect
                                          :attrs {:x "100"
                                                  :y "100"
                                                  :width "100"
                                                  :height "100"
                                                  :fill "red"}}])
      (rf/dispatch [::element.events/->path])

      (rf.test/wait-for
       [::element.events/finalize->path]

       (is (= (-> @selected first :tag) :path))
       (is (= (-> @selected first :attrs :fill) "red"))
       (not (-> @selected first :attrs :x)))))))

(deftest stroke->path
  (rf.test/run-test-async
   (rf/dispatch-sync [::app.events/initialize])

   (rf.test/wait-for
    [::app.events/set-loading false]

    (let [selected (rf/subscribe [::element.subs/selected])]
      (rf/dispatch [::element.events/add {:tag :rect
                                          :attrs {:x "100"
                                                  :y "100"
                                                  :width "100"
                                                  :height "100"
                                                  :fill "red"
                                                  :stroke "black"}}])
      (rf/dispatch [::element.events/stroke->path])

      (rf.test/wait-for
       [::element.events/finalize-stroke->path]

       (is (= (-> @selected first :tag) :path))
       (is (= (-> @selected first :attrs :fill) "black"))
       (not (-> @selected first :attrs :stroke)))))))

(deftest boolean-operation
  (rf.test/run-test-async
   (rf/dispatch-sync [::app.events/initialize])

   (rf.test/wait-for
    [::app.events/set-loading false]

    (let [selected (rf/subscribe [::element.subs/selected])]
      (rf/dispatch [::element.events/add {:tag :rect
                                          :attrs {:x "100"
                                                  :y "100"
                                                  :width "100"
                                                  :height "100"
                                                  :fill "red"
                                                  :stroke "black"}}])
      (rf/dispatch [::element.events/add {:tag :rect
                                          :attrs {:x "100"
                                                  :y "100"
                                                  :width "100"
                                                  :height "100"}}])
      (rf/dispatch [::element.events/select-all])
      (rf/dispatch [::element.events/boolean-operation :unite])

      (rf.test/wait-for
       [::element.events/finalize-boolean-operation]

       (is (= (-> @selected first :tag) :path))
       (is (= (-> @selected first :attrs :fill) "red")))))))

(deftest import-svg
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [selected (rf/subscribe [::element.subs/selected])]
     (rf/dispatch [::element.events/import-svg
                   {:svg "<svg x=\"10\" width=\"200\" height=\"200\"></svg>"
                    :label "filename.svg"
                    :position [500 500]}])
     (is (= (-> @selected first :tag) :svg))
     (is (= (-> @selected first :label) "filename.svg"))
     (is (= (-> @selected first :attrs :x) "500"))
     (is (= (-> @selected first :attrs :width) "200")))))

(deftest animate
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (rf/dispatch [::element.events/add {:tag :rect
                                       :attrs {:x "100"
                                               :y "100"
                                               :width "100"
                                               :height "100"}}])
   (let [selected (rf/subscribe [::element.subs/selected])
         id (-> @selected first :id)]
     (rf/dispatch [::element.events/animate :animate {}])
     (is (= (-> @selected first :tag) :animate))
     (is (= (-> @selected first :parent) id)))))

(deftest set-parent
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (rf/dispatch [::element.events/add {:tag :rect
                                       :attrs {:x "100"
                                               :y "100"
                                               :width "100"
                                               :height "100"}}])
   (let [selected (rf/subscribe [::element.subs/selected])
         id (-> @selected first :id)
         root (rf/subscribe [::element.subs/root])]
     (is (not= (-> @selected first :parent) (:id @root)))

     (rf/dispatch [::element.events/set-parent id (:id @root)])
     (is (= (-> @selected first :parent) (:id @root))))))

(deftest group
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (rf/dispatch [::element.events/add {:tag :rect
                                       :attrs {:x "100"
                                               :y "100"
                                               :width "100"
                                               :height "100"}}])
   (let [selected (rf/subscribe [::element.subs/selected])
         id (-> @selected first :id)]
     (testing "group"
       (rf/dispatch [::element.events/group])
       (is (= (-> @selected first :tag) :g))
       (is (= (-> @selected first :children) [id])))

     (testing "ungroup"
       (rf/dispatch [::element.events/ungroup])
       (is (= (-> @selected first :tag) :rect))))))

(deftest fonts
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])
   (rf/dispatch [::app.events/load-system-fonts])

   (rf/dispatch [::element.events/add {:tag :text
                                       :content "Hello!"
                                       :attrs {:x "100"
                                               :y "100"}}])
   (let [font-weights (rf/subscribe [::element.subs/font-weights])]
     (testing "no available font weights"
       (is (= @font-weights #{})))

     (testing "bold weight"
       (rf/dispatch [::element.events/set-attr :font-family "Adwaita Mono"])
       (is (= @font-weights #{"700"}))))))

(deftest combine-and-break-apart
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [selected (rf/subscribe [::element.subs/selected])]
     (rf/dispatch [::element.events/add {:tag :path
                                         :attrs {:d "M0 0L 100 100"}}])
     (rf/dispatch [::element.events/add {:tag :path
                                         :attrs {:d "M100 0 L0 100"}}])

     (rf/dispatch [::element.events/select-all])

     (testing "combine"
       (rf/dispatch [::element.events/combine])
       (is (= (-> @selected first :attrs :d) "M0 0L100 100 M100 0L0 100")))

     (testing "break apart"
       (rf/dispatch [::element.events/break-apart])
       (is (= (-> @selected first :attrs :d) "M 0 0 L 100 100"))
       (is (= (-> @selected second :attrs :d) "M 100 0 L 0 100"))))))
