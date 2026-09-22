(ns utils.color-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.utils.color :as utils.color]))

(deftest string->notation
  (testing "string to notation"
    (are [s notation] (= (utils.color/string->notation s) notation)
      "" "hex"
      "#000000" "hex"
      "black" "hex"
      "rgb(0, 0, 0)" "rgb"
      "rgba(208, 47, 101, 0.5)" "rgb"
      "hsl(0, 0%, 0%)" "hsl"
      "hsla(0, 0%, 0%, 0.5)" "hsl")))

(deftest string->color
  (testing "string to color"
    (are [s expected] (= (utils.color/->css (utils.color/string->color s))
                         expected)
      "" "#000000"
      "black" "#000000"
      "red" "#ff0000"
      "#0f0" "#00ff00"
      "rgb(255, 0, 0)" "#ff0000"
      "hsl(0, 100%, 50%)" "#ff0000"
      "not-a-color" "#000000")
    (are [s default expected] (= (-> (utils.color/string->color s default)
                                     (utils.color/->css))
                                 expected)
      "not-a-color" "red" "#ff0000"
      "not-a-color" "#00ff00" "#00ff00")))

(deftest ->css
  (testing "color to css"
    (let [red (utils.color/hsl->color 0 1 0.5)
          black (utils.color/hsl->color 0 0 0)]
      (are [color expected] (= (utils.color/->css color) expected)
        red "#ff0000"
        black "#000000")
      (are [color notation expected] (= (utils.color/->css color notation)
                                        expected)
        red "hex" "#ff0000"
        red "rgb" "rgb(255 0 0)"
        red "hsl" "hsl(0deg 100% 50%)"
        red "lab" "lab(54.29% 80.81 69.89)"
        red "lch" "lch(54.29% 106.84 40.85deg)"
        red "oklch" "oklch(62.8% 0.26 29.23deg)"
        red "oklab" "oklab(62.8% 0.22 0.13)"
        black "hex" "#000000"
        black "rgb" "rgb(0 0 0)"
        black "hsl" "hsl(0deg 0% 0%)"
        black "lab" "lab(0% 0 0)"
        black "lch" "lch(0% 0 none)"
        black "oklch" "oklch(0% 0 none)"
        black "oklab" "oklab(0% 0 0)"))))

(deftest set-alpha
  (testing "set alpha"
    (let [red (utils.color/hsl->color 0 1 0.5)]
      (are [notation expected] (= (utils.color/set-alpha red notation 0.5)
                                  expected)
        "hex" "#ff000080"
        "rgb" "rgb(255 0 0 / 0.5)"
        "hsl" "hsl(0deg 100% 50% / 0.5)"
        "lab" "lab(54.29% 80.81 69.89 / 0.5)"
        "lch" "lch(54.29% 106.84 40.85deg / 0.5)"
        "oklch" "oklch(62.8% 0.26 29.23deg / 0.5)"
        "oklab" "oklab(62.8% 0.22 0.13 / 0.5)"))))

(deftest set-hue
  (testing "set hue"
    (let [red (utils.color/hsl->color 0 1 0.5)
          black (utils.color/hsl->color 0 0 0)]
      (are [color hue expected] (= (utils.color/set-hue color "hex" hue)
                                   expected)
        red 0 "#ff0000"
        red 120 "#00ff00"
        red 240 "#0000ff"
        black 120 "#000000"))))

(deftest valid-channel-value?
  (testing "valid channel value"
    (are [channel v valid] (= (utils.color/valid-channel-value? channel v)
                              valid)
      "hex" "#ff0000" true
      "hex" "black" true
      "hex" "not-a-color" false
      "r" "128" true
      "r" "12.5" true
      "r" "0" true
      "r" "" false
      "r" "  " false
      "r" "abc" false
      "alpha" "0.5" true
      "alpha" "abc" false)))

(deftest index->channel
  (testing "index to channel"
    (are [index notation channel] (= (utils.color/index->channel index notation)
                                     channel)
      0 "hex" "hex"
      3 "hex" "hex"
      0 "rgb" "r"
      1 "rgb" "g"
      2 "rgb" "b"
      3 "rgb" "alpha"
      0 "hsl" "h"
      1 "hsl" "s"
      2 "hsl" "l"
      3 "hsl" "alpha"
      0 "lab" "l"
      1 "lab" "a"
      2 "lab" "b"
      0 "lch" "l"
      1 "lch" "c"
      2 "lch" "h"
      0 "oklch" "l"
      1 "oklch" "c"
      2 "oklch" "h"
      0 "oklab" "l"
      1 "oklab" "a"
      2 "oklab" "b"
      4 "oklab" "alpha")))

(deftest channel-values
  (testing "channel values"
    (let [red (utils.color/hsl->color 0 1 0.5)
          black (utils.color/hsl->color 0 0 0)
          white (utils.color/hsl->color 0 0 1)]
      (are [color notation values] (= (-> color
                                          (utils.color/channel-values notation)
                                          (vec))
                                      values)
        red "hex" ["#ff0000"]
        red "rgb" [255 0 0 1]
        red "hsl" [0 1 0.5 1]
        red "lab" [53.240788867616104 80.09249428641473 67.20319139735453]
        red "lch" [53.240788867616104 104.55178896130302 39.998996244225]
        red "oklch" [0.6279886791592625 0.2576969748097739 29.23388027962784]
        red "oklab" [0.6279886791592625 0.22487499820191523 0.12585295391764884]
        black "hex" ["#000000"]
        black "rgb" [0 0 0 1]
        black "lab" [0 0 0]
        white "hex" ["#ffffff"]
        white "rgb" [255 255 255 1]))))
