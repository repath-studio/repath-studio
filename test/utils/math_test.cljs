(ns utils.math-test
  (:require
   [cljs.test :refer-macros [deftest testing is]]
   [clojure.test.check :as tc]
   [clojure.test.check.properties :as prop]
   [generators :as generators]
   [renderer.utils.math :as utils.math]))

(deftest test-clamp
  (testing "clamp keeps value within bounds and is idempotent"
    (is (tc/quick-check 100
                        (prop/for-all [x generators/coord
                                       a generators/coord
                                       b generators/coord]
                                      (let [lo (min a b)
                                            hi (max a b)
                                            c (utils.math/clamp x lo hi)
                                            c2 (utils.math/clamp c lo hi)]
                                        (and (<= lo c hi)
                                             (= c c2))))))))

(deftest test-normalize-angle
  (testing "normalize-angle returns a value in [0, 2*PI)"
    (is (tc/quick-check 100
                        (prop/for-all [x generators/coord]
                                      (let [a (utils.math/normalize-angle x)]
                                        (and (>= a 0)
                                             (< a (* 2 Math/PI)))))))))

(deftest test-normalize-angle-idempotent
  (testing "normalize-angle is idempotent"
    (is (tc/quick-check 100
                        (prop/for-all [x generators/coord]
                                      (= (utils.math/normalize-angle x)
                                         (utils.math/normalize-angle
                                          (utils.math/normalize-angle x))))))))

(deftest test-distance
  (testing "distance is non-negative and symmetric"
    (is (tc/quick-check 100
                        (prop/for-all [p generators/point q generators/point]
                                      (let [d1 (utils.math/distance p q)
                                            d2 (utils.math/distance q p)]
                                        (and (>= d1 0)
                                             (= d1 d2))))))))

(deftest test-triangle-inequality
  (testing "triangle inequality holds for distance"
    (is (tc/quick-check 100
                        (prop/for-all [p generators/point
                                       q generators/point
                                       r generators/point]
                                      (<= (utils.math/distance p r)
                                          (+ (utils.math/distance p q)
                                             (utils.math/distance q r))
                                          1e-10))))))

(deftest test-v-add-commutative
  (testing "v-add is commutative"
    (is (tc/quick-check 100
                        (prop/for-all [a generators/point b generators/point]
                                      (= (utils.math/v-add a b)
                                         (utils.math/v-add b a)))))))

(deftest test-v-add-identity
  (testing "v-add with zero vector is identity"
    (is (tc/quick-check 100
                        (prop/for-all [a generators/point]
                                      (= a (utils.math/v-add a [0 0])))))))

(deftest test-v-sub-zero
  (testing "v-sub of a vector with itself is zero"
    (is (tc/quick-check 100
                        (prop/for-all [a generators/point]
                                      (every? zero? (utils.math/v-sub a a)))))))
