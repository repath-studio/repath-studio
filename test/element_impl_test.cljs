(ns element-impl-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [clojure.string :as string]
   [renderer.element.hierarchy :as element.hierarchy]))

(def test-cases
  [{:label "circle"
    :el {:type :element
         :tag :circle
         :attrs {:cx "0"
                 :cy "0"
                 :r "50"}}
    :translate {:offset [50 50]
                :result {:cx "50"
                         :cy "50"
                         :r "50"}}
    :scale [{:pivot [50 50]
             :result {:cx "0"
                      :cy "0"
                      :r "100"}}
            {:pivot [0 0]
             :result {:cx "50"
                      :cy "50"
                      :r "100"}}]
    :bbox [-50 -50 50 50]
    :path (string/join " " ["M 50 0"
                            "C 50 27.614 27.614 50 0 50"
                            "S -50 27.614 -50 0"
                            "S -27.614 -50 0 -50"
                            "S 50 -27.614 50 0"
                            "z"])}

   {:label "rect"
    :el {:type :element
         :tag :rect
         :attrs {:x "0"
                 :y "0"
                 :width "50"
                 :height "50"}}
    :translate {:offset [50 50]
                :result {:x "50"
                         :y "50"
                         :width "50"
                         :height "50"}}
    :scale [{:pivot [25 25]
             :result {:x "-25"
                      :y "-25"
                      :width "100"
                      :height "100"}}
            {:pivot [0 0]
             :result {:x "0"
                      :y "0"
                      :width "100"
                      :height "100"}}]
    :bbox [0 0 50 50]
    :path (string/join " " ["M 0 0" "L 50 0" "L 50 50" "L 0 50" "z"])}

   {:label "ellipse"
    :el {:type :element
         :tag :ellipse
         :attrs {:cx "0"
                 :cy "0"
                 :rx "50"
                 :ry "50"}}
    :translate {:offset [50 50]
                :result {:cx "50"
                         :cy "50"
                         :rx "50"
                         :ry "50"}}
    :scale [{:pivot [25 25]
             :result {:cx "25"
                      :cy "25"
                      :rx "100"
                      :ry "100"}}
            {:pivot [0 0]
             :result {:cx "50"
                      :cy "50"
                      :rx "100"
                      :ry "100"}}]
    :bbox [-50 -50 50 50]
    :path (string/join " " ["M 50 0"
                            "C 50 27.614 27.614 50 0 50"
                            "S -50 27.614 -50 0"
                            "S -27.614 -50 0 -50"
                            "S 50 -27.614 50 0"
                            "z"])}

   {:label "line"
    :el {:type :element
         :tag :line
         :attrs {:x1 "0"
                 :y1 "0"
                 :x2 "50"
                 :y2 "50"}}
    :translate {:offset [50 50]
                :result {:x1 "50"
                         :y1 "50"
                         :x2 "100"
                         :y2 "100"}}
    :scale [{:pivot [25 25]
             :result {:x1 "-25"
                      :y1 "-25"
                      :x2 "75"
                      :y2 "75"}}
            {:pivot [0 0]
             :result {:x1 "0"
                      :y1 "0"
                      :x2 "100"
                      :y2 "100"}}]
    :bbox [0 0 50 50]
    :path "M 0 0 L 50 50"}

   {:label "polygon"
    :el {:type :element
         :tag :polygon
         :attrs {:points "528 -305 718 -370 941 -208"}}
    :translate {:offset [10 10]
                :result {:points "538 -295 728 -360 951 -198"}}
    :scale [{:pivot [25 25]
             :result {:points "503 -265 883 -395 1329 -71"}}
            {:pivot [0 0]
             :result {:points "528 -240 908 -370 1354 -46"}}]
    :bbox [528 -370 941 -208]
    :path "M528 -305 718 -370 941 -208z"
    :snapping-points [[528 -305] [718 -370] [941 -208]]}

   {:label "polyline"
    :el {:type :element
         :tag :polyline
         :attrs {:points "528 -305 718 -370 941 -208"}}
    :translate {:offset [10 10]
                :result {:points "538 -295 728 -360 951 -198"}}
    :scale [{:pivot [25 25]
             :result {:points "503 -265 883 -395 1329 -71"}}
            {:pivot [0 0]
             :result {:points "528 -240 908 -370 1354 -46"}}]
    :bbox [528 -370 941 -208]
    :path "M528 -305 718 -370 941 -208"
    :snapping-points [[528 -305] [718 -370] [941 -208]]}

   {:label "path"
    :el {:type :element
         :tag :path
         :attrs {:d "M528 -305 718 -371 941 -208 663 -174 664 -261z"}}
    :translate {:offset [10 10]
                :result {:d "M538-295L728-361 951-198 673-164 674-251z"}}
    :scale [{:pivot [25 25]
             :result {:d "M503-264L883-396 1329-70 773-2 775-176z"}}
            {:pivot [0 0]
             :result {:d "M528-239L908-371 1354-45 798 23 800-151z"}}]
    :bbox [528 -371 941 -174]}

   {:label "svg"
    :el {:type :element
         :tag :svg
         :attrs {:x "0"
                 :y "0"
                 :width "50"
                 :height "50"}}
    :translate {:offset [50 50]
                :result {:x "50"
                         :y "50"
                         :width "50"
                         :height "50"}}
    :scale [{:pivot [25 25]
             :result {:x "-25"
                      :y "-25"
                      :width "100"
                      :height "100"}}
            {:pivot [0 0]
             :result {:x "0"
                      :y "0"
                      :width "100"
                      :height "100"}}]
    :bbox [0 0 50 50]
    :path-throws? true}

   {:label "text"
    :el {:type :element
         :tag :text
         :content "My text"
         :attrs {:x "0"
                 :y "0"
                 :width "50"
                 :height "50"}}
    :translate {:offset [50 50]
                :result {:x "50"
                         :y "50"
                         :width "50"
                         :height "50"}}}])

(deftest translating
  (doseq [{:keys [label el translate]} test-cases]
    (testing label
      (is (= (:attrs (element.hierarchy/translate el (:offset translate)))
             (:result translate))))))

(deftest scaling
  (doseq [{:keys [label el scale]} test-cases
          :when scale
          {:keys [pivot result]} scale]
    (testing (str label " at " pivot)
      (is (= (:attrs (element.hierarchy/scale el [2 2] pivot))
             result)))))

(deftest bounding-box
  (doseq [{:keys [label el bbox]} test-cases
          :when bbox]
    (testing label
      (is (= (element.hierarchy/bbox el) bbox)))))

(deftest to-path
  (doseq [{:keys [label el path path-throws?]} test-cases
          :when (or path path-throws?)]
    (testing label
      (if path-throws?
        (is (thrown? js/Error (element.hierarchy/path el)))
        (is (= (element.hierarchy/path el) path))))))

(deftest snapping
  (doseq [{:keys [label el snapping-points]} test-cases
          :when snapping-points]
    (testing label
      (is (= (element.hierarchy/snapping-points el) snapping-points)))))
