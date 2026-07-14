(ns renderer.reepl.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::shell
 :-> :shell)

(rf/reg-sub
 ::verbose?
 :<- [::shell]
 :-> :verbose)

(rf/reg-sub
 ::active-language
 :<- [::shell]
 :-> :active-language)

(rf/reg-sub
 ::language-status
 :<- [::shell]
 :-> :language-status)

(rf/reg-sub
 ::active-language-status
 :<- [::language-status]
 :<- [::active-language]
 (fn [[language-status active-language] _]
   (get language-status active-language)))
