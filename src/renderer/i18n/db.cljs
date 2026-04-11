(ns renderer.i18n.db
  (:require
   [malli.core :as m]))

(def Translation
  [:* any?])

(def LanguageDirection
  [:enum "ltr" "rtl"])

(def LanguageCode
  [:re #"^[A-Z]{2}"])

(def LanguageCodeIdentifier
  [:re #"^[a-z]{2}-[A-Z]{2}$"])

(def LanguageId
  [:or LanguageCodeIdentifier [:= "system"]])

(def Language
  [:map {:closed true}
   [:locale string?]
   [:id LanguageCodeIdentifier]
   [:dir LanguageDirection]
   [:code LanguageCode]
   [:dictionary map?]])

(def valid-language? (m/validator Language))

(def explain-language (m/explainer Language))

(def LanguageRegistry
  [:map-of LanguageCodeIdentifier Language])
