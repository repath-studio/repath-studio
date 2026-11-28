(ns snap-test
  (:require
   [cljs.test :refer-macros [deftest are is testing]]
   [day8.re-frame.test :as rf.test]
   [kdtree :as kdtree]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.document.events :as-alias document.events]
   [renderer.element.events :as-alias element.events]
   [renderer.frame.events :as-alias frame.events]
   [renderer.input.events :as-alias input.events]
   [renderer.snap.events :as-alias snap.events]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.snap.subs :as-alias snap.subs]
   [renderer.tool.events :as-alias tool.events]))

(def frame
  {:x 252
   :y 139
   :width 1946
   :height 945
   :top 139
   :right 2198
   :bottom 1084
   :left 252})

(defn setup-scene
  [attrs]
  (rf/dispatch [::document.events/new-from-template [800 600]])
  (rf/dispatch [::frame.events/resize frame])
  (rf/dispatch [::tool.events/set-state :translate])
  (rf/dispatch [::element.events/add {:tag :rect
                                      :attrs {:x "100"
                                              :y "0"
                                              :width "200"
                                              :height "200"}}])
  (rf/dispatch [::element.events/deselect-all])
  (rf/dispatch [::element.events/add {:tag :rect
                                      :attrs attrs}]))

(defn translate!
  []
  (rf/dispatch [::tool.events/set-state :translate])
  (rf/dispatch [::input.events/pointer
                {:element nil
                 :target nil
                 :pointer-pos [155 130]
                 :pressure 0.5
                 :pointer-type "mouse"
                 :pointer-id 1
                 :timestamp 1000
                 :primary true
                 :button nil
                 :alt-key false
                 :ctrl-key false
                 :meta-key false
                 :shift-key false
                 :type "pointermove"}]))

(deftest options
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [active? (rf/subscribe [::snap.subs/active?])
         opts (rf/subscribe [::snap.subs/options])
         snapping (rf/subscribe [::snap.subs/snapping])
         nearest-x (rf/subscribe [::snap.subs/nearest-x])
         nearest-y (rf/subscribe [::snap.subs/nearest-y])]
     (testing "default state"
       (is @active?)
       (is (= @opts #{:centers :midpoints :corners
                      :alignment :grid :guides}))
       (is (empty? @snapping))
       (is (nil? @nearest-x))
       (is (nil? @nearest-y)))

     (testing "toggle snap"
       (rf/dispatch [::snap.events/toggle])
       (is (not @active?))
       (rf/dispatch [::snap.events/toggle])
       (is @active?))

     (testing "toggle options"
       (are [option]
            (do
              (rf/dispatch [::snap.events/toggle-option option])
              (is (not @(rf/subscribe [::snap.subs/option-enabled? option])))
              (rf/dispatch [::snap.events/toggle-option option])
              (is @(rf/subscribe [::snap.subs/option-enabled? option])))
         :alignment
         :grid
         :guides)))))

(deftest tree
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])
   (rf/dispatch [::document.events/new-from-template [800 600]])
   (rf/dispatch [::frame.events/resize frame])
   (rf/dispatch [::tool.events/set-state :translate])
   (rf/dispatch [::element.events/add {:tag :rect
                                       :attrs {:x "100"
                                               :y "0"
                                               :width "200"
                                               :height "200"}}])
   (rf/dispatch [::element.events/deselect-all])

   (let [snapping (rf/subscribe [::snap.subs/snapping])]
     (testing "tree built from unselected elements"
       (is (:kdtree @snapping))
       (is (:viewbox-kdtree @snapping))
       (is (:x-tree @snapping))
       (is (:y-tree @snapping)))

     (testing "tree cleared when snap is disabled"
       (rf/dispatch [::snap.events/toggle])
       (is (empty? @snapping)))

     (testing "tree rebuilt when snap is re-enabled"
       (rf/dispatch [::snap.events/toggle])
       (is (:kdtree @snapping))))))

(deftest axis-match
  (let [snap-tree (kdtree/build-tree
                   (list (with-meta [100]
                           {:id nil
                            :label [::grid-line "grid line"]
                            :point [100 200]})))]
    (testing "match within threshold"
      (let [{:keys [axis value delta dist point]}
            (snap.handlers/axis-match :x [105 0] snap-tree 169)]
        (is (= axis :x))
        (is (= value 100))
        (is (= delta -5))
        (is (= dist 25))
        (is (= point [100 200]))))

    (testing "no match outside threshold"
      (is (nil? (snap.handlers/axis-match :x [120 0] snap-tree 169))))))

(deftest snap
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [nearest-x (rf/subscribe [::snap.subs/nearest-x])
         nearest-y (rf/subscribe [::snap.subs/nearest-y])
         nearest-neighbor (rf/subscribe [::snap.subs/nearest-neighbor])]

     (testing "axis snapping"
       (setup-scene {:x "105"
                     :y "80"
                     :width "100"
                     :height "100"})
       (translate!)
       (is (nil? @nearest-neighbor))
       (is (nil? @nearest-y))
       (is (= (:axis @nearest-x) :x))
       (is (= (:delta @nearest-x) -5)))

     (testing "point snapping takes precedence over axis snapping"
       (setup-scene {:x "105"
                     :y "5"
                     :width "100"
                     :height "100"})
       (translate!)
       (is (nil? @nearest-x))
       (is (nil? @nearest-y))
       (let [{:keys [point base-point dist-squared]} @nearest-neighbor]
         (is (= dist-squared 50))
         (is (= (map - point base-point) [-5 -5]))))

     (testing "no snapping candidates"
       (setup-scene {:x "135"
                     :y "80"
                     :width "100"
                     :height "100"})
       (translate!)
       (is (nil? @nearest-neighbor))
       (is (nil? @nearest-x))
       (is (nil? @nearest-y))))))
