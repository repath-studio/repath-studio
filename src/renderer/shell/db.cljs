(ns renderer.shell.db
  (:require
   [malli.core :as m]
   [malli.transform :as m.transform]
   [renderer.db :refer [LoadingState]]
   [renderer.hierarchy :as hierarchy]
   [renderer.shell.hierarchy :as shell.hierarchy]))

(defn shell-language?
  [k]
  (contains? (descendants @hierarchy/hierarchy ::shell.hierarchy/language) k))

(def ShellLanguageId
  [:fn {:error/fn (fn [{:keys [value]} _]
                    (str value ", is not a supported language"))}
   shell-language?])

(def ShellItemType
  [:enum :input :output :error])

(def ShellItem
  [:multi {:dispatch :type}
   [:error
    [:map {:closed true}
     [:type [:= :error]]
     [:value [:map {:closed true}
              [:via [:map {:closed true}
                     [:type any?]
                     [:message any?]
                     [:data any?]
                     [:at any?]]]
              [:trace any?]
              [:cause any?]
              [:data any?]
              [:phase any?]]]]]
   [:input
    [:map {:closed true}
     [:type [:= :input]]
     [:num {:optional true} int?]
     [:value string?]]]
   [:output
    [:map {:closed true}
     [:type [:= :output]]
     [:value string?]]]])

(def ShellHistory
  [:vector string?])

(def ShellLanguage
  [:map {:closed true}
   [:status {:optional true} LoadingState]
   [:history {:default [""]} ShellHistory]
   [:hist-pos {:default 0} int?]
   [:items {:default []} [:vector ShellItem]]])

(def Shell
  [:map {:closed true}
   [:verbose {:default false} boolean?]
   [:languages {:default {}} [:map-of ShellLanguageId ShellLanguage]]
   [:active-language {:default :cljs} keyword?]])

(def default-lang (m/decode ShellLanguage
                            {}
                            m.transform/default-value-transformer))
