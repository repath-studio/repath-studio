(ns utils.bounds-test
  (:require
   [cljs.test :refer-macros [deftest testing are is]]
   [clojure.test.check :as tc]
   [clojure.test.check.properties :as prop]
   [generators :as generators]
   [renderer.i18n.views :refer [t]]
   [renderer.utils.bounds :as utils.bounds]))

(defn union-associative? [a b c]
  (= (utils.bounds/union (utils.bounds/union a b) c)
     (utils.bounds/union a (utils.bounds/union b c))))

(defn symmetric? [f]
  (tc/quick-check 100
                  (prop/for-all [a generators/bbox b generators/bbox]
                                (= (f a b) (f b a)))))

(deftest test-union
  (testing "united bounds"
    (are [x y] (= x y)
      [0 0 20 20] (utils.bounds/union [0 0 10 10] [11 11 20 20])
      [0 0 20 20] (utils.bounds/union [0 0 10 10] [9 9 20 20])
      [0 0 11 11] (utils.bounds/union [11 11 11 11] [0 0 0 0])))

  (testing "union is commutative"
    (is (tc/quick-check 100
                        (prop/for-all [a generators/bbox b generators/bbox]
                                      (= (utils.bounds/union a b)
                                         (utils.bounds/union b a))))))

  (testing "union with self is idempotent"
    (is (tc/quick-check 100
                        (prop/for-all [a generators/bbox]
                                      (= (utils.bounds/union a a) a)))))

  (testing "union is associative"
    (is (tc/quick-check 100
                        (prop/for-all [a generators/bbox
                                       b generators/bbox
                                       c generators/bbox]
                                      (union-associative? a b c))))))

(deftest test-intersect?
  (testing "bounds are intersecting"
    (is (false? (utils.bounds/intersect? [0 0 10 10] [11 11 20 20])))
    (is (true? (utils.bounds/intersect? [0 0 10 10] [9 9 20 20])))
    (is (true? (utils.bounds/intersect? [0 0 10 10] [10 10 11 11]))))

  (testing "intersect? is symmetric"
    (is (symmetric? utils.bounds/intersect?))))

(deftest test-contained?
  (testing "bounds are contained"
    (is (false? (utils.bounds/contained? [0 0 10 10] [0 0 10 10])))
    (is (true? (utils.bounds/contained? [5 5 10 10] [0 0 20 20])))
    (is (false? (utils.bounds/contained? [0 0 10 10] [1 1 10 10]))))

  (testing "contained? implies intersect?"
    (is (tc/quick-check 100
                        (prop/for-all [a generators/bbox b generators/bbox]
                                      (when (utils.bounds/contained? a b)
                                        (utils.bounds/intersect? a b)))))))

(deftest test-contained-point?
  (testing "bounds contain point"
    (is (true? (utils.bounds/contained-point? [0 0 10 10] [0 0])))
    (is (true? (utils.bounds/contained-point? [0 0 10 10] [5 5])))
    (is (true? (utils.bounds/contained-point? [0 0 10 10] [10 10])))
    (is (false? (utils.bounds/contained-point? [0 0 10 10] [-5 5])))
    (is (false? (utils.bounds/contained-point? [0 0 10 10] [5 -5])))))

(deftest test-center
  (testing "center of bounds"
    (is (= (utils.bounds/center [0 0 10 10]) [5 5])))

  (testing "center of a bbox is within the bbox"
    (is (tc/quick-check 100
                        (prop/for-all [bbox generators/bbox]
                                      (utils.bounds/contained-point?
                                       bbox (utils.bounds/center bbox)))))))

(deftest test-->snapping-points
  (with-redefs [t (fn [& _] "translation")]
    (testing "snapping points of bounds"
      (is (= (utils.bounds/->snapping-points [0 0 10 10]
                                             #{:corners :centers :midpoints})
             [[0 0] [0 10] [10 0] [10 10] [5 5] [0 5] [10 5] [5 0] [5 10]]))

      (is (= (utils.bounds/->snapping-points [0 0 10 10] #{}) [])))))
