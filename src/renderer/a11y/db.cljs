(ns renderer.a11y.db
  (:require
   [malli.core :as m]
   [renderer.i18n.db :refer [Translation]]))

(def A11yFilterId keyword?)

(def FilterTag
  [:enum
   :feSpotLight
   :feBlend
   :feColorMatrix
   :feComponentTransfer
   :feComposite
   :feConvolveMatrix
   :feDiffuseLighting
   :feDisplacementMap
   :feDropShadow
   :feFlood
   :feGaussianBlur
   :feImage
   :feMerge
   :feMorphology
   :feOffset
   :feSpecularLighting
   :feTile
   :feTurbulence])

(def A11yFilter
  [:multi {:dispatch :tag}
   [:feGaussianBlur
    [:map {:closed true}
     [:id A11yFilterId]
     [:tag [:= :feGaussianBlur]]
     [:label Translation]
     [:attrs [:map
              [:in {:optional true} string?]
              [:stdDeviation string?]
              [:edgeMode {:optional true} string?]]]]]
   [:feColorMatrix
    [:map {:closed true}
     [:id A11yFilterId]
     [:tag [:= :feColorMatrix]]
     [:label Translation]
     [:attrs [:map
              [:in {:optional true} string?]
              [:type {:optional true} string?]
              [:values string?]]]]]
   [::m/default
    [:map {:closed true}
     [:id A11yFilterId]
     [:tag FilterTag]
     [:label Translation]
     [:attrs [:map-of keyword? string?]]]]])

(def A11y
  [:map
   [:filters [:vector A11yFilter]]
   [:active-filter {:optional true} A11yFilterId]])

(def valid-filter? (m/validator A11yFilter))

(def explain-filter (m/explainer A11yFilter))

(def default-filters
  [{:id :blur
    :tag :feGaussianBlur
    :label [::blur "blur"]
    :attrs {:in "SourceGraphic"
            :stdDeviation "1"}}
   {:id :blur-x2
    :tag :feGaussianBlur
    :label [::heavy-blur "blur-x2"]
    :attrs {:in "SourceGraphic"
            :stdDeviation "2"}}
   ;; https://github.com/hail2u/color-blindness-emulation
   {:id :protanopia
    :tag :feColorMatrix
    :label [::protanopia "Protanopia"]
    :attrs {:in "SourceGraphic"
            :type "matrix"
            :values "0.567, 0.433, 0, 0, 0
                     0.558, 0.442, 0, 0, 0
                     0, 0.242, 0.758, 0, 0
                     0, 0, 0, 1, 0"}}
   {:id :protanomaly
    :tag :feColorMatrix
    :label [::protanomaly "Protanomaly"]
    :attrs {:in "SourceGraphic"
            :type "matrix"
            :values "0.817, 0.183, 0, 0, 0
                     0.333, 0.667, 0, 0, 0
                     0, 0.125, 0.875, 0, 0
                     0, 0, 0, 1, 0"}}
   {:id :deuteranopia
    :tag :feColorMatrix
    :label [::deuteranopia "Deuteranopia"]
    :attrs {:in "SourceGraphic"
            :type "matrix"
            :values "0.625, 0.375, 0, 0, 0
                     0.7, 0.3, 0, 0, 0
                     0, 0.3, 0.7, 0, 0
                     0, 0, 0, 1, 0"}}
   {:id :deuteranomaly
    :tag :feColorMatrix
    :label [::deuteranomaly "Deuteranomaly"]
    :attrs {:in "SourceGraphic"
            :type "matrix"
            :values "0.8, 0.2, 0, 0, 0
                     0.258, 0.742, 0, 0, 0
                     0, 0.142, 0.858, 0, 0
                     0, 0, 0, 1, 0"}}
   {:id :tritanopia
    :tag :feColorMatrix
    :label [::tritanopia "Tritanopia"]
    :attrs {:in "SourceGraphic"
            :type "matrix"
            :values "0.95, 0.05, 0, 0, 0
                     0, 0.433, 0.567, 0, 0
                     0, 0.475, 0.525, 0, 0
                     0, 0, 0, 1, 0"}}
   {:id :tritanomaly
    :tag :feColorMatrix
    :label [::tritanomaly "Tritanomaly"]
    :attrs {:in "SourceGraphic"
            :type "matrix"
            :values "0.967, 0.033, 0, 0, 0
                     0, 0.733, 0.267, 0, 0
                     0, 0.183, 0.817, 0, 0
                     0, 0, 0, 1, 0"}}
   {:id :achromatopsia
    :tag :feColorMatrix
    :label [::achromatopsia "Achromatopsia"]
    :attrs {:in "SourceGraphic"
            :type "matrix"
            :values "0.299, 0.587, 0.114, 0, 0
                     0.299, 0.587, 0.114, 0, 0
                     0.299, 0.587, 0.114, 0, 0
                     0, 0, 0, 1, 0"}}
   {:id :achromatomaly
    :tag :feColorMatrix
    :label [::achromatomaly "Achromatomaly"]
    :attrs {:in "SourceGraphic"
            :type "matrix"
            :values "0.618, 0.320, 0.062, 0, 0
                     0.163, 0.775, 0.062, 0, 0
                     0.163, 0.320, 0.516, 0, 0
                     0, 0, 0, 1, 0"}}])
