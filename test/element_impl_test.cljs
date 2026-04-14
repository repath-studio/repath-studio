(ns element-impl-test
  (:require
   [cljs.test :refer-macros [deftest is]]
   [clojure.string :as string]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.impl.core]))

(deftest circle
  (let [circle-el {:type :element
                   :tag :circle
                   :attrs {:cx "0"
                           :cy "0"
                           :r "50"}}]

    (is (= (:attrs (element.hierarchy/translate circle-el [50 50]))
           {:cx "50"
            :cy "50"
            :r "50"}))

    (is (= (:attrs (element.hierarchy/scale circle-el [2 2] [50 50]))
           {:cx "0"
            :cy "0"
            :r "100"}))

    (is (= (:attrs (element.hierarchy/scale circle-el [2 2] [0 0]))
           {:cx "50"
            :cy "50"
            :r "100"}))

    (is (= (element.hierarchy/bbox circle-el)
           [-50 -50 50 50]))

    (is (= (element.hierarchy/path circle-el)
           (string/join " " ["M 50 0"
                             "A 50 50 0 0 1 0 50"
                             "A 50 50 0 0 1 -50 0"
                             "A 50 50 0 0 1 50 0"
                             "z"])))))

(deftest rect
  (let [rect-el {:type :element
                 :tag :rect
                 :attrs {:x "0"
                         :y "0"
                         :width "50"
                         :height "50"}}]

    (is (= (:attrs (element.hierarchy/translate rect-el [50 50]))
           {:x "50"
            :y "50"
            :width "50"
            :height "50"}))

    (is (= (:attrs (element.hierarchy/scale rect-el [2 2] [25 25]))
           {:x "-25"
            :y "-25"
            :width "100"
            :height "100"}))

    (is (= (:attrs (element.hierarchy/scale rect-el [2 2] [0 0]))
           {:x "0"
            :y "0"
            :width "100"
            :height "100"}))

    (is (= (element.hierarchy/bbox rect-el)
           [0 0 50 50]))

    (is (= (element.hierarchy/path rect-el)
           (string/join " " ["M 0 0"
                             "H 50 V 50"
                             "H 0"
                             "V 0"
                             "z"])))))

(deftest ellipse
  (let [ellipse-el {:type :element
                    :tag :ellipse
                    :attrs {:cx "0"
                            :cy "0"
                            :rx "50"
                            :ry "50"}}]

    (is (= (:attrs (element.hierarchy/translate ellipse-el [50 50]))
           {:cx "50"
            :cy "50"
            :rx "50"
            :ry "50"}))

    (is (= (:attrs (element.hierarchy/scale ellipse-el [2 2] [25 25]))
           {:cx "25"
            :cy "25"
            :rx "100"
            :ry "100"}))

    (is (= (:attrs (element.hierarchy/scale ellipse-el [2 2] [0 0]))
           {:cx "50"
            :cy "50"
            :rx "100"
            :ry "100"}))

    (is (= (element.hierarchy/bbox ellipse-el)
           [-50 -50 50 50]))

    (is (= (element.hierarchy/path ellipse-el)
           (string/join " " ["M 50 0"
                             "A 50 50 0 0 1 0 50"
                             "A 50 50 0 0 1 -50 0"
                             "A 50 50 0 0 1 50 0"
                             "z"])))))

(deftest line
  (let [line-el {:type :element
                 :tag :line
                 :attrs {:x1 "0"
                         :y1 "0"
                         :x2 "50"
                         :y2 "50"}}]

    (is (= (:attrs (element.hierarchy/translate line-el [50 50]))
           {:x1 "50"
            :y1 "50"
            :x2 "100"
            :y2 "100"}))

    (is (= (:attrs (element.hierarchy/scale line-el [2 2] [25 25]))
           {:x1 "-25"
            :y1 "-25"
            :x2 "75"
            :y2 "75"}))

    (is (= (:attrs (element.hierarchy/scale line-el [2 2] [0 0]))
           {:x1 "0"
            :y1 "0"
            :x2 "100"
            :y2 "100"}))

    (is (= (element.hierarchy/bbox line-el)
           [0 0 50 50]))

    (is (= (element.hierarchy/path line-el)
           "M 0 0 L 50 50"))))

(deftest polygon
  (let [polygon-el {:type :element
                    :tag :polygon
                    :attrs {:points "528 -305 718 -370 941 -208"}}]

    (is (= (:attrs (element.hierarchy/translate polygon-el [10 10]))
           {:points "538 -295 728 -360 951 -198"}))

    (is (= (:attrs (element.hierarchy/scale polygon-el [2 2] [25 25]))
           {:points "503 -265 883 -395 1329 -71"}))

    (is (= (:attrs (element.hierarchy/scale polygon-el [2 2] [0 0]))
           {:points "528 -240 908 -370 1354 -46"}))

    (is (= (element.hierarchy/bbox polygon-el)
           [528 -370 941 -208]))

    (is (= (element.hierarchy/path polygon-el)
           "M528 -305 718 -370 941 -208z"))

    (is (= (element.hierarchy/snapping-points polygon-el)
           [[528 -305] [718 -370] [941 -208]]))))

(deftest polyline
  (let [polyline-el {:type :element
                     :tag :polyline
                     :attrs {:points "528 -305 718 -370 941 -208"}}]

    (is (= (:attrs (element.hierarchy/translate polyline-el [10 10]))
           {:points "538 -295 728 -360 951 -198"}))

    (is (= (:attrs (element.hierarchy/scale polyline-el [2 2] [25 25]))
           {:points "503 -265 883 -395 1329 -71"}))

    (is (= (:attrs (element.hierarchy/scale polyline-el [2 2] [0 0]))
           {:points "528 -240 908 -370 1354 -46"}))

    (is (= (element.hierarchy/bbox polyline-el)
           [528 -370 941 -208]))

    (is (= (element.hierarchy/path polyline-el)
           "M528 -305 718 -370 941 -208"))

    (is (= (element.hierarchy/snapping-points polyline-el)
           [[528 -305] [718 -370] [941 -208]]))))

(deftest path
  (let [path-el {:type :element
                 :tag :path
                 :attrs {:d "M528 -305 718 -371 941 -208 663 -174 664 -261z"}}]

    (is (= (:attrs (element.hierarchy/translate path-el [10 10]))
           {:d "M538-295L728-361 951-198 673-164 674-251z"}))

    (is (= (:attrs (element.hierarchy/scale path-el [2 2] [25 25]))
           {:d "M503-264L883-396 1329-70 773-2 775-176z"}))

    (is (= (:attrs (element.hierarchy/scale path-el [2 2] [0 0]))
           {:d "M528-239L908-371 1354-45 798 23 800-151z"}))

    (is (= (element.hierarchy/bbox path-el)
           [528 -371 941 -174]))))

(deftest svg
  (let [svg-el {:type :element
                :tag :svg
                :attrs {:x "0"
                        :y "0"
                        :width "50"
                        :height "50"}}]

    (is (= (:attrs (element.hierarchy/translate svg-el [50 50]))
           {:x "50"
            :y "50"
            :width "50"
            :height "50"}))

    (is (= (:attrs (element.hierarchy/scale svg-el [2 2] [25 25]))
           {:x "-25"
            :y "-25"
            :width "100"
            :height "100"}))

    (is (= (:attrs (element.hierarchy/scale svg-el [2 2] [0 0]))
           {:x "0"
            :y "0"
            :width "100"
            :height "100"}))

    (is (= (element.hierarchy/bbox svg-el)
           [0 0 50 50]))

    (is (thrown? js/Error (element.hierarchy/path svg-el)))))

(deftest text
  (let [text-el {:type :element
                 :tag :text
                 :content "My text"
                 :attrs {:x "0"
                         :y "0"
                         :width "50"
                         :height "50"}}]

    (is (= (:attrs (element.hierarchy/translate text-el [50 50]))
           {:x "50"
            :y "50"
            :width "50"
            :height "50"}))))
