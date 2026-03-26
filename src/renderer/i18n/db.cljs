(ns renderer.i18n.db
  (:require
   [malli.core :as m]
   [taoensso.tempura :refer-macros [load-resource-at-compile-time]]))

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

(def languages
  [{:id "en-US"
    :dir "ltr"
    :locale "English"
    :code "EN"
    :dictionary (load-resource-at-compile-time "lang/en-US.edn")}
   {:id "es-ES"
    :dir "ltr"
    :locale "Español"
    :code "ES"
    :dictionary (load-resource-at-compile-time "lang/es-ES.edn")}
   {:id "pt-PT"
    :dir "ltr"
    :locale "Português"
    :code "PT"
    :dictionary (load-resource-at-compile-time "lang/pt-PT.edn")}
   {:id "ru-RU"
    :dir "ltr"
    :locale "Русский"
    :code "RU"
    :dictionary (load-resource-at-compile-time "lang/ru-RU.edn")}
   {:id "zh-CN"
    :dir "ltr"
    :locale "中文（简体）"
    :code "ZH"
    :dictionary (load-resource-at-compile-time "lang/zh-CN.edn")}
   {:id "fr-FR"
    :dir "ltr"
    :locale "Français"
    :code "FR"
    :dictionary (load-resource-at-compile-time "lang/fr-FR.edn")}
   {:id "de-DE"
    :dir "ltr"
    :locale "Deutsch"
    :code "DE"
    :dictionary (load-resource-at-compile-time "lang/de-DE.edn")}
   {:id "el-GR"
    :dir "ltr"
    :locale "Ελληνικά"
    :code "EL"
    :dictionary (load-resource-at-compile-time "lang/el-GR.edn")}
   {:id "ar-EG"
    :dir "rtl"
    :locale "العربية (مصر)"
    :code "AR"
    :dictionary (load-resource-at-compile-time "lang/ar-EG.edn")}
   {:id "ja-JP"
    :dir "ltr"
    :locale "日本語"
    :code "JA"
    :dictionary (load-resource-at-compile-time "lang/ja-JP.edn")}
   {:id "ko-KR"
    :dir "ltr"
    :locale "한국어"
    :code "KO"
    :dictionary (load-resource-at-compile-time "lang/ko-KR.edn")}
   {:id "tr-TR"
    :dir "ltr"
    :locale "Türkçe"
    :code "TR"
    :dictionary (load-resource-at-compile-time "lang/tr-TR.edn")}
   {:id "it-IT"
    :dir "ltr"
    :locale "Italiano"
    :code "IT"
    :dictionary (load-resource-at-compile-time "lang/it-IT.edn")}
   {:id "nl-NL"
    :dir "ltr"
    :locale "Nederlands"
    :code "NL"
    :dictionary (load-resource-at-compile-time "lang/nl-NL.edn")}
   {:id "sv-SE"
    :dir "ltr"
    :locale "Svenska"
    :code "SV"
    :dictionary (load-resource-at-compile-time "lang/sv-SE.edn")}])

(def registry
  (zipmap (map :id languages) languages))
