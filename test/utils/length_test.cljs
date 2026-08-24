(ns utils.length-test
  (:require
   [cljs.test :refer-macros [deftest testing is]]
   [renderer.utils.length :as utils.length]))

(deftest test-valid-unit?
  (testing "check if unit is valid"
    (is (true? (utils.length/valid-unit? "px")))
    (is (true? (utils.length/valid-unit? "em")))
    (is (true? (utils.length/valid-unit? "rem")))
    (is (false? (utils.length/valid-unit? "foo")))
    (is (false? (utils.length/valid-unit? "")))))
