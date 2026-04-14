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
 (fn [user-lang [_ lang]]
   (= user-lang lang)))

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
 (fn [[languages user-lang system-lang] _]
   (i18n.handlers/computed-lang languages user-lang system-lang)))

(rf/reg-sub
 ::options
 :<- [::languages]
 i18n.handlers/tempura-options)

(rf/reg-sub
 ::language
 :<- [::languages]
 :<- [::lang]
 (fn [[languages lang] _]
   (get languages lang)))

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
 (fn [[languages system-lang] _]
   (get languages system-lang)))

(rf/reg-sub
 ::system-lang-code
 :<- [::system-language]
 :-> :code)
