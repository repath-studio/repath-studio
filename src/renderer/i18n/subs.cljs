(ns renderer.i18n.subs
  (:require
   [re-frame.core :as rf]
   [renderer.i18n.handlers :as i18n.handlers]))

(rf/reg-sub
 ::user-lang
 :-> :user-lang)

(rf/reg-sub
 ::selected-lang?
 :<- [::user-lang]
 :=> =)

(rf/reg-sub
 ::system-lang
 :-> :system-lang)

(rf/reg-sub
 ::languages
 :-> :languages)

(rf/reg-sub
 ::lang
 :<- [::languages]
 :<- [::user-lang]
 :<- [::system-lang]
 :-> (partial apply i18n.handlers/computed-lang))

(rf/reg-sub
 ::options
 :<- [::languages]
 :-> i18n.handlers/tempura-options)

(rf/reg-sub
 ::language
 :<- [::languages]
 :<- [::lang]
 :-> (partial apply get))

(rf/reg-sub
 ::lang-dir
 :<- [::language]
 :-> :dir)

(rf/reg-sub
 ::lang-code
 :<- [::language]
 :-> :code)

(rf/reg-sub
 ::system-language
 :<- [::languages]
 :<- [::system-lang]
 :-> (partial apply get))

(rf/reg-sub
 ::system-lang-code
 :<- [::system-language]
 :-> :code)
