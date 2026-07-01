(ns utils.extra-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.utils.extra :refer [rpartial]]))

(deftest test-rpartial
  (testing "right partial"
    (are [x y] (= x y)
      [1 0 2 0 3 0] (reduce (rpartial conj 0) [] [1 2 3])
      "1a2a3a" (reduce (rpartial str "a") "" [1 2 3]))))
