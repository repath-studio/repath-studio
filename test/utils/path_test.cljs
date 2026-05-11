(ns utils.path-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.utils.path :as utils.path]))

(deftest test-segment->command
  (testing "getting a path command from a segment"
    (are [x y] (= x y)
      nil (utils.path/segment->command nil)
      "M" (utils.path/segment->command ["M" 10 10])
      "M" (utils.path/segment->command ["m" 10 10])
      "L" (utils.path/segment->command ["l" 20 20]))))

(deftest test-segment-point
  (testing "getting a position for a segment and point type"
    (are [x y] (= x y)
      nil (utils.path/segment-point nil :end-point)
      [10 10] (utils.path/segment-point ["M" 10 10] :end-point)
      [20 20] (utils.path/segment-point ["C" 5 5 15 15 20 20] :end-point)
      [5 5] (utils.path/segment-point ["C" 5 5 15 15 20 20]
                                      :start-control-point))))

(deftest test-string->segments
  (testing "parsing path data string into segments"
    (are [x y] (= x y)
      nil (utils.path/string->segments nil)
      [] (utils.path/string->segments "")
      [["M" 10 10]] (utils.path/string->segments "M10 10")
      [["M" 10 10] ["L" 20 20]] (utils.path/string->segments "M10 10 L20 20"))))

(deftest test-segments->string
  (testing "converting segments back to path data string"
    (are [x y] (= x y)
      "" (utils.path/segments->string [])
      "M 10 10" (utils.path/segments->string [["M" 10 10]])
      "M 10 10 L 20 20 z" (utils.path/segments->string [["M" 10 10]
                                                        ["L" 20 20]
                                                        ["z"]]))))

(deftest test-drop-last-segment
  (testing "dropping the last segment from segments vector"
    (are [x y] (= x y)
      [] (utils.path/drop-last-segment [])
      [["M" 10 10]] (utils.path/drop-last-segment [["M" 10 10]])
      [["M" 10 10]] (utils.path/drop-last-segment [["M" 10 10] ["L" 20 20]]))))
