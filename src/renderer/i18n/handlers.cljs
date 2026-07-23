(ns renderer.i18n.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.i18n.db :refer [LanguageCodeIdentifier Language LanguageRegistry
                             LanguageId Translation]]
   [taoensso.tempura :as tempura]))

(m/=> register-language [:-> App Language App])
(defn register-language
  [db language]
  (assoc-in db [:languages (:id language)] language))

(m/=> deregister-language [:-> App LanguageCodeIdentifier App])
(defn deregister-language
  [db id]
  (update db :languages dissoc id))

(m/=> set-translation [:-> App LanguageCodeIdentifier keyword? Translation App])
(defn set-translation
  [db lang-id k v]
  (cond-> db
    (get-in db [:languages lang-id])
    (assoc-in [:languages lang-id :dictionary k] v)))

(m/=> supported-lang? [:->
                       LanguageRegistry [:maybe LanguageCodeIdentifier]
                       boolean?])
(defn supported-lang?
  [languages lang]
  (contains? languages lang))

(m/=> computed-lang [:->
                     LanguageRegistry LanguageId [:maybe LanguageCodeIdentifier]
                     LanguageCodeIdentifier])
(defn computed-lang
  [languages user-lang system-lang]
  (if (or (not user-lang)
          (= user-lang "system"))
    (if (supported-lang? languages system-lang)
      system-lang
      "en-US")
    user-lang))

(m/=> tempura-options [:-> LanguageRegistry map?])
(defn tempura-options
  [languages]
  {:dict (->> languages
              (map (fn [[k v]] [k (:dictionary v)]))
              (into {}))})

(m/=> translate [:-> map? LanguageCodeIdentifier Translation any?])
(defn translate
  [options lang translation]
  (apply tempura/tr options [lang] translation))

(m/=> t [:-> App Translation any?])
(defn t
  "Translation function that can be called outside of a reactive context."
  [db & more]
  (let [{:keys [user-lang system-lang languages]} db
        lang (computed-lang languages user-lang system-lang)]
    (translate (tempura-options languages) lang more)))
