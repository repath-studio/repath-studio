(ns renderer.a11y.core
  (:require
   [re-frame.core :as rf]
   [renderer.a11y.events :as a11y.events]
   [renderer.a11y.subs]
   [renderer.action.events :as-alias action.events]))

(rf/dispatch [::action.events/register-action-group
              {:id :a11y/filter
               :label [::accessibility-filter "Accessibility filter"]
               :actions []}])

(rf/dispatch [::a11y.events/register-filter
              {:id :blur
               :tag :feGaussianBlur
               :label [::blur "blur"]
               :attrs {:in "SourceGraphic"
                       :stdDeviation "1"}}])

(rf/dispatch [::a11y.events/register-filter
              {:id :blur-x2
               :tag :feGaussianBlur
               :label [::heavy-blur "blur-x2"]
               :attrs {:in "SourceGraphic"
                       :stdDeviation "2"}}])

;; https://github.com/hail2u/color-blindness-emulation

(rf/dispatch [::a11y.events/register-filter
              {:id :protanopia
               :tag :feColorMatrix
               :label [::protanopia "Protanopia"]
               :attrs {:in "SourceGraphic"
                       :type "matrix"
                       :values "0.567, 0.433, 0, 0, 0
                                0.558, 0.442, 0, 0, 0
                                0, 0.242, 0.758, 0, 0
                                0, 0, 0, 1, 0"}}])

(rf/dispatch [::a11y.events/register-filter
              {:id :protanomaly
               :tag :feColorMatrix
               :label [::protanomaly "Protanomaly"]
               :attrs {:in "SourceGraphic"
                       :type "matrix"
                       :values "0.817, 0.183, 0, 0, 0
                                0.333, 0.667, 0, 0, 0
                                0, 0.125, 0.875, 0, 0
                                0, 0, 0, 1, 0"}}])

(rf/dispatch [::a11y.events/register-filter
              {:id :deuteranopia
               :tag :feColorMatrix
               :label [::deuteranopia "Deuteranopia"]
               :attrs {:in "SourceGraphic"
                       :type "matrix"
                       :values "0.625, 0.375, 0, 0, 0
                                0.7, 0.3, 0, 0, 0
                                0, 0.3, 0.7, 0, 0
                                0, 0, 0, 1, 0"}}])

(rf/dispatch [::a11y.events/register-filter
              {:id :deuteranomaly
               :tag :feColorMatrix
               :label [::deuteranomaly "Deuteranomaly"]
               :attrs {:in "SourceGraphic"
                       :type "matrix"
                       :values "0.8, 0.2, 0, 0, 0
                                0.258, 0.742, 0, 0, 0
                                0, 0.142, 0.858, 0, 0
                                0, 0, 0, 1, 0"}}])

(rf/dispatch [::a11y.events/register-filter
              {:id :tritanopia
               :tag :feColorMatrix
               :label [::tritanopia "Tritanopia"]
               :attrs {:in "SourceGraphic"
                       :type "matrix"
                       :values "0.95, 0.05, 0, 0, 0
                                0, 0.433, 0.567, 0, 0
                                0, 0.475, 0.525, 0, 0
                                0, 0, 0, 1, 0"}}])

(rf/dispatch [::a11y.events/register-filter
              {:id :tritanomaly
               :tag :feColorMatrix
               :label [::tritanomaly "Tritanomaly"]
               :attrs {:in "SourceGraphic"
                       :type "matrix"
                       :values "0.967, 0.033, 0, 0, 0
                                0, 0.733, 0.267, 0, 0
                                0, 0.183, 0.817, 0, 0
                                0, 0, 0, 1, 0"}}])

(rf/dispatch [::a11y.events/register-filter
              {:id :achromatopsia
               :tag :feColorMatrix
               :label [::achromatopsia "Achromatopsia"]
               :attrs {:in "SourceGraphic"
                       :type "matrix"
                       :values "0.299, 0.587, 0.114, 0, 0
                                0.299, 0.587, 0.114, 0, 0
                                0.299, 0.587, 0.114, 0, 0
                                0, 0, 0, 1, 0"}}])

(rf/dispatch [::a11y.events/register-filter
              {:id :achromatomaly
               :tag :feColorMatrix
               :label [::achromatomaly "Achromatomaly"]
               :attrs {:in "SourceGraphic"
                       :type "matrix"
                       :values "0.618, 0.320, 0.062, 0, 0
                                0.163, 0.775, 0.062, 0, 0
                                0.163, 0.320, 0.516, 0, 0
                                0, 0, 0, 1, 0"}}])
