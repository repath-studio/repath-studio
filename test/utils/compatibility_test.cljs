(ns utils.compatibility-test
  (:require
   [cljs.test :refer-macros [deftest testing are is]]
   [renderer.utils.compatibility :as utils.compatibility]))

(deftest test-version->vec
  (testing "version to vector"
    (are [x y] (= x y)
      [0 3 3] (utils.compatibility/version->vec "0.3.3-2-4cd3bf6-SNAPSHOT")
      [123 0 3] (utils.compatibility/version->vec "123.0.3-32423423"))))

(deftest test-requires-migration?
  (testing "migration requirement"
    (is (true? (utils.compatibility/requires-migration? {} [0 4 0])))
    (is (true? (utils.compatibility/requires-migration? {:version "0.3.3"}
                                                        [0 4 0])))
    (is (true? (utils.compatibility/requires-migration? {:version "0.3.3"}
                                                        [0 3 4])))
    (is (false? (utils.compatibility/requires-migration? {:version "1.3.3"}
                                                         [0 3 4])))
    (is (false? (utils.compatibility/requires-migration? {:version "1.3.3"}
                                                         [1 3 3])))))
