(ns utils.vec-test
  (:require
   [cljs.test :refer-macros [deftest testing are is]]
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [renderer.utils.vec :as utils.vec]))

(def gen-vec (gen/vector gen/keyword 1 10))

(def gen-vec-with-index
  (gen/bind gen-vec (fn [v]
                      (gen/fmap (fn [i] [v i])
                                (gen/choose 0 (dec (count v)))))))

(deftest test-remove-nth
  (testing "removing bth element of vector"
    (are [x y] (= x y)
      [:a :b :d] (utils.vec/remove-nth [:a :b :c :d] 2)
      [:a :b :c] (utils.vec/remove-nth [:a :b :c :d] 3))))

(deftest test-add
  (testing "adding an element to index"
    (are [x y] (= x y)
      [:a :b :c :d] (utils.vec/add [:a :b :c] 3 :d)
      [:a :x :b :c :d] (utils.vec/add [:a :b :c :d] 1 :x))))

(deftest test-move
  (testing "moving element by index"
    (are [x y] (= x y)
      [:a :b :d :c] (utils.vec/move [:a :b :c :d] 2 3)
      [:a :b :c :d] (utils.vec/move [:a :b :c :d] 1 1)))

  (testing "move(v, i, i) = v"
    (is (tc/quick-check 100
                        (prop/for-all [[v i] gen-vec-with-index]
                                      (= (utils.vec/move v i i) v))))))

(defn swap-involution? [v i j]
  (when (and (< i (count v)) (< j (count v)))
    (= (utils.vec/swap (utils.vec/swap v i j) i j) v)))

(deftest test-swap
  (testing "swapping elements by index"
    (are [x y] (= x y)
      [:d :b :c :a] (utils.vec/swap [:a :b :c :d] 0 3)
      [:a :b :c :d] (utils.vec/swap [:a :b :c :d] 1 1)))

  (testing "swap(swap(v, i, j), i, j) = v"
    (is (tc/quick-check 100
                        (prop/for-all [v gen-vec
                                       i (gen/choose 0 9)
                                       j (gen/choose 0 9)]
                                      (swap-involution? v i j)))))

  (testing "swap(v, i, i) = v"
    (is (tc/quick-check 100
                        (prop/for-all [[v i] gen-vec-with-index]
                                      (= (utils.vec/swap v i i) v))))))

(deftest test-remove-nth-add-roundtrip
  (testing "add(remove-nth(v, i), i, v[i]) = v"
    (is (tc/quick-check
         100
         (prop/for-all [[v i] gen-vec-with-index]
                       (= (utils.vec/add (utils.vec/remove-nth v i) i (nth v i))
                          v))))))
