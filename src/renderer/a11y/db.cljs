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
