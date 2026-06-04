(ns utils.path-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [goog.object]
   [renderer.utils.path :as utils.path]))

(deftest test-segment->command
  (testing "getting a path command from a segment"
    (are [x y] (= x y)
      nil (utils.path/segment->command nil)
      "M" (utils.path/segment->command #js ["M" 10 10])
      "M" (utils.path/segment->command #js ["m" 10 10])
      "L" (utils.path/segment->command #js ["l" 20 20]))))

(deftest test-segment-point
  (testing "getting a position for a segment and point type"
    (are [x y] (= x y)
      nil (utils.path/segment-point nil :end-point)
      [10 10] (utils.path/segment-point #js ["M" 10 10] :end-point)
      [20 20] (utils.path/segment-point #js ["C" 5 5 15 15 20 20] :end-point)
      [5 5] (utils.path/segment-point #js ["C" 5 5 15 15 20 20]
                                      :start-control-point))))

(deftest test-string->segments
  (testing "parsing path data string into segments"
    (are [x y] (= (js->clj x) (js->clj y))
      nil (utils.path/string->segments nil)
      [] (into [] (utils.path/string->segments ""))
      #js [#js ["M" 10 10]] (utils.path/string->segments "M10 10")
      #js [#js ["M" 10 10]
           #js ["L" 20 20]] (utils.path/string->segments "M10 10 L20 20"))))

(deftest test-drop-last-segment
  (testing "dropping the last segment from segments vector"
    (are [x y] (= (js->clj x) (js->clj y))
      #js [] (utils.path/drop-last-segment #js [])
      #js [#js ["M" 10 10]] (utils.path/drop-last-segment #js [#js ["M" 10 10]])
      #js [#js ["M" 10 10]] (utils.path/drop-last-segment
                             #js [#js ["M" 10 10]
                                  #js ["L" 20 20]]))))
