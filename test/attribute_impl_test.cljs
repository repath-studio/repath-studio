(ns attribute-impl-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [renderer.attribute.hierarchy :as attribute.hierarchy]))

(deftest length
  (let [rect-el {:type :element
                 :tag :rect
                 :attrs {:x "100"
                         :width "10"
                         :height "5px"}}]

    (testing "update existing length attributes"
      (is (= (attribute.hierarchy/update-attr rect-el :x + 50)
             {:type :element
              :tag :rect
              :attrs {:x "150"
                      :width "10"
                      :height "5px"}}))

      (is (= (attribute.hierarchy/update-attr rect-el :x - 150)
             {:type :element
              :tag :rect
              :attrs {:x "-50"
                      :width "10"
                      :height "5px"}})))

    (testing "update non-existing length attribute"
      (is (= (attribute.hierarchy/update-attr rect-el :y + 50)
             {:type :element
              :tag :rect
              :attrs {:x "100"
                      :y "50"
                      :width "10"
                      :height "5px"}})))))

(deftest positive-length
  (let [rect-el {:type :element
                 :tag :rect
                 :attrs {:x "100"
                         :width "10"
                         :height "5px"}}]

    (testing "update positive length without unit"
      (is (= (attribute.hierarchy/update-attr rect-el :width + 2)
             {:type :element
              :tag :rect
              :attrs {:x "100"
                      :width "12"
                      :height "5px"}})))

    (testing "clamp negative length to zero"
      (is (= (attribute.hierarchy/update-attr rect-el :width - 100)
             {:type :element
              :tag :rect
              :attrs {:x "100"
                      :width "0"
                      :height "5px"}})))

    (testing "update positive length with unit"
      (is (= (attribute.hierarchy/update-attr rect-el :height + 2)
             {:type :element
              :tag :rect
              :attrs {:x "100"
                      :width "10"
                      :height "7px"}})))))
