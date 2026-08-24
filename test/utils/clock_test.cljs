(ns utils.clock-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.utils.clock :as utils.clock]))

(deftest parsing
  (testing "parsing timecount values"
    (are [x y] (= (utils.clock/->ms x) y)
      "" nil
      "1min.20sec" nil
      "12foo" nil
      "123minutes" nil
      "0" 0
      "1000ms" 1000
      "10min" 600000
      "1.5h" 5400000
      "44s" 44000
      "45.345" 45345))

  (testing "parsing full clock values"
    (are [x y] (= (utils.clock/->ms x) y)
      "::" nil
      "20:30:40:50" nil
      "00:00:00.25" 250
      "02:30:03" 9003000
      "12:00:00" 43200000))

  (testing "parsing partial clock values"
    (are [x y] (= (utils.clock/->ms x) y)
      "01:10" 70000
      "00:22.1" 22100)))
