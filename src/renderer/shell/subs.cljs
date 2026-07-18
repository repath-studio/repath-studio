(ns renderer.shell.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::shell
 :-> :shell)

(rf/reg-sub
 ::active-language
 :<- [::shell]
 :-> :active-language)

(rf/reg-sub
 ::active-language?
 :<- [::active-language]
 :=> =)

(rf/reg-sub
 ::languages
 :<- [::shell]
 :-> :languages)

(rf/reg-sub
 ::language
 :<- [::languages]
 :<- [::active-language]
 :-> (partial apply get))

(rf/reg-sub
 ::items
 :<- [::language]
 :-> :items)

(rf/reg-sub
 ::history-pos
 :<- [::language]
 :-> :history-pos)

(rf/reg-sub
 ::language-status
 :<- [::language]
 :-> :status)

(rf/reg-sub
 ::language-loaded?
 :<- [::language-status]
 :-> (partial = :success))

(rf/reg-sub
 ::current-text
 :<- [::language]
 :<- [::history-pos]
 :-> (fn [[language hist-pos] _]
       (let [history (:history language)
             pos (- (count history) hist-pos 1)]
         (get history pos))))
