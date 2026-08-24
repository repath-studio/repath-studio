(ns utils.attribute-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.i18n.views :refer [t]]
   [renderer.utils.attribute :as utils.attribute]))

(deftest test-str->seq
  (testing "string to sequence conversion"
    (are [x y] (= x y)
      ["0" "1"]
      (utils.attribute/str->seq "0 1")

      ["0" "12" "342.3" "4352345345" "454535" "34"]
      (utils.attribute/str->seq "0 12 342.3 4352345345 454535 34"))))

(deftest test-points->vec
  (testing "string to point vector conversion"
    (are [x y] (= x y)
      [["0" "12"] ["342.3" "4352345345"] ["454535" "34"]]
      (utils.attribute/points->vec "0 12 342.3 4352345345 454535 34")

      [["0" "12"] ["342.3" "4352345345"] ["454535" "34"] ["1"]]
      (utils.attribute/points->vec "0 12 342.3 4352345345 454535 34 1"))))

(deftest test->camel-case
  (testing "attribute key to camel-case conversion"
    (are [x y] (= x y)
      :viewBox (utils.attribute/->camel-case :viewbox)
      :glyphOrientationHorizontal (utils.attribute/->camel-case
                                   :Glyphorientationhorizontal))))

(deftest test->camel-case-memo
  (testing "memoized attribute key to camel-case conversion"
    (are [x y] (= x y)
      :viewBox (utils.attribute/->camel-case :viewbox)
      :glyphOrientationHorizontal (utils.attribute/->camel-case
                                   :Glyphorientationhorizontal))))

(deftest test-defaults
  (with-redefs [t (fn [& _] "translation")]
    (testing "default tag attributes"
      (are [x y] (= x y)
        {:x ""
         :y ""
         :rx ""
         :ry ""
         :width ""
         :height ""
         :fill ""
         :stroke ""
         :stroke-dasharray ""
         :stroke-linejoin ""
         :style ""
         :stroke-width ""
         :opacity ""
         :id ""
         :class ""} (utils.attribute/defaults :rect)))))

(deftest test-to-fixed
  (testing "round to precision"
    (are [x y] (= x y)
      "1.111" (utils.attribute/->fixed 1.111111 3)
      "1" (utils.attribute/->fixed 1 3)
      "1.000" (utils.attribute/->fixed 1 3 false)
      "1.11" (utils.attribute/->fixed 1.111 2)
      "1.11" (utils.attribute/->fixed 1.114 2)
      "1.12" (utils.attribute/->fixed 1.116 2)
      "1.11" (utils.attribute/->fixed 1.11100 2)
      "1" (utils.attribute/->fixed 1 0)
      "0" (utils.attribute/->fixed 0 2))))
