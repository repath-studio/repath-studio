(ns renderer.i18n.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.i18n.effects]
   [renderer.i18n.events :as i18n.events]
   [renderer.i18n.subs :as i18n.subs]
   [taoensso.tempura :refer-macros [load-resource-at-compile-time]]))

(rf/dispatch [::action.events/register-action
              {:id :lang/system
               :label [::system "System"]
               :icon "language"
               :event [::i18n.events/set-user-lang "system"]
               :active [::i18n.subs/selected-lang? "system"]}])

(rf/dispatch [::action.events/register-action-group
              {:id :i18n/language
               :label [::language "Language"]
               :actions [:lang/system]}])

(rf/dispatch [::i18n.events/register-language
              {:id "en-US"
               :dir "ltr"
               :locale "English"
               :code "EN"
               :dictionary (load-resource-at-compile-time "lang/en-US.edn")}])

(rf/dispatch [::i18n.events/register-language
              {:id "es-ES"
               :dir "ltr"
               :locale "Español"
               :code "ES"
               :dictionary (load-resource-at-compile-time "lang/es-ES.edn")}])

(rf/dispatch [::i18n.events/register-language
              {:id "pt-PT"
               :dir "ltr"
               :locale "Português"
               :code "PT"
               :dictionary (load-resource-at-compile-time "lang/pt-PT.edn")}])

(rf/dispatch [::i18n.events/register-language
              {:id "ru-RU"
               :dir "ltr"
               :locale "Русский"
               :code "RU"
               :dictionary (load-resource-at-compile-time "lang/ru-RU.edn")}])

(rf/dispatch [::i18n.events/register-language
              {:id "zh-CN"
               :dir "ltr"
               :locale "中文（简体）"
               :code "ZH"
               :dictionary (load-resource-at-compile-time "lang/zh-CN.edn")}])

(rf/dispatch [::i18n.events/register-language
              {:id "fr-FR"
               :dir "ltr"
               :locale "Français"
               :code "FR"
               :dictionary (load-resource-at-compile-time "lang/fr-FR.edn")}])

(rf/dispatch [::i18n.events/register-language
              {:id "de-DE"
               :dir "ltr"
               :locale "Deutsch"
               :code "DE"
               :dictionary (load-resource-at-compile-time "lang/de-DE.edn")}])

(rf/dispatch [::i18n.events/register-language
              {:id "el-GR"
               :dir "ltr"
               :locale "Ελληνικά"
               :code "EL"
               :dictionary (load-resource-at-compile-time "lang/el-GR.edn")}])

(rf/dispatch [::i18n.events/register-language
              {:id "ar-EG"
               :dir "rtl"
               :locale "العربية (مصر)"
               :code "AR"
               :dictionary (load-resource-at-compile-time "lang/ar-EG.edn")}])

(rf/dispatch [::i18n.events/register-language
              {:id "ja-JP"
               :dir "ltr"
               :locale "日本語"
               :code "JA"
               :dictionary (load-resource-at-compile-time "lang/ja-JP.edn")}])

(rf/dispatch [::i18n.events/register-language
              {:id "ko-KR"
               :dir "ltr"
               :locale "한국어"
               :code "KO"
               :dictionary (load-resource-at-compile-time "lang/ko-KR.edn")}])

(rf/dispatch [::i18n.events/register-language
              {:id "tr-TR"
               :dir "ltr"
               :locale "Türkçe"
               :code "TR"
               :dictionary (load-resource-at-compile-time "lang/tr-TR.edn")}])

(rf/dispatch [::i18n.events/register-language
              {:id "it-IT"
               :dir "ltr"
               :locale "Italiano"
               :code "IT"
               :dictionary (load-resource-at-compile-time "lang/it-IT.edn")}])

(rf/dispatch [::i18n.events/register-language
              {:id "nl-NL"
               :dir "ltr"
               :locale "Nederlands"
               :code "NL"
               :dictionary (load-resource-at-compile-time "lang/nl-NL.edn")}])

(rf/dispatch [::i18n.events/register-language
              {:id "sv-SE"
               :dir "ltr"
               :locale "Svenska"
               :code "SV"
               :dictionary (load-resource-at-compile-time "lang/sv-SE.edn")}])
