(ns renderer.shell.subs
  (:require
   [re-frame.core :as rf]
   [renderer.shell.hierarchy :as shell.hierarchy]))

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
 ::some-items?
 :<- [::items]
 :-> (comp boolean seq))

(rf/reg-sub
 ::history
 :<- [::language]
 :-> :history)

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
 :<- [::history]
 :<- [::history-pos]
 :-> (fn [[history history-pos] _]
       (let [pos (dec (- (count history) history-pos))]
         (get history pos))))

(rf/reg-sub
 ::completion
 :<- [::shell]
 :-> :completion)

(rf/reg-sub
 ::completion-pos
 :<- [::completion]
 :-> :pos)

(rf/reg-sub
 ::completion-words
 :<- [::completion]
 :-> :words)

(rf/reg-sub
 ::completion-active?
 :<- [::completion]
 :-> :active)

(rf/reg-sub
 ::completion-show-all?
 :<- [::completion]
 :-> :show-all)

(rf/reg-sub
 ::completion-initial-text
 :<- [::completion]
 :-> :initial-text)

(rf/reg-sub
 ::active-completion
 :<- [::completion-words]
 :<- [::completion-pos]
 :-> (partial apply get))

(rf/reg-sub
 ::docs
 :<- [::active-language]
 :<- [::active-completion]
 :-> (fn [[active-language active-completion] _]
       (shell.hierarchy/docs active-language (first active-completion))))

(rf/reg-sub
 ::cycle-completions?
 :<- [::completion-words]
 :<- [::completion-initial-text]
 :-> (fn [words initial-text]
       (and (seq words)
            (or (< 1 (count words))
                (and (< 0 (count words))
                     (not= initial-text (get (first words) 2)))))))
