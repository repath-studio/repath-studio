(ns utils.element-test
  (:require
   [cljs.test :refer-macros [deftest testing are is]]
   [renderer.element.impl.core]
   [renderer.utils.element :as utils.element]))

(deftest test-root?
  (testing "is root element"
    (is (true? (utils.element/root? {:type :element
                                     :tag :canvas})))
    (is (false? (utils.element/root? {:type :element
                                      :tag :rect})))))

(deftest test-normalize
  (testing "element normalization"
    (is (= (utils.element/normalize {:tag :text
                                     :content []
                                     :foo nil
                                     :locked true
                                     :attrs {:x 10
                                             :y "20"}})
           {:tag :text
            :type :element
            :visible true
            :children []
            :selected-handles #{}
            :attrs {:x "10"
                    :y "20"}}))))

(deftest test-scale-offset
  (testing "scale offset"
    (are [x y] (= x y)
      [-10 -40] (utils.element/scale-offset [2 3] [10 20])
      [0 0] (utils.element/scale-offset [1 1] [1 1]))))
