(ns renderer.shell.db
  (:require
   [config :as config]
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

(def ShellItem
  [:multi {:dispatch :type}
   [:input
    [:map {:closed true}
     [:type [:= :input]]
     [:value [:map {:closed true}
              [:current-ns any?]
              [:num int?]
              [:text string?]]]]]
   [:output
    [:map {:closed true}
     [:type [:= :output]]
     [:value any?]]]
   [:error
    [:map {:closed true}
     [:type [:= :error]]
     [:value [:map
              [:trace {:optional true} any?]
              [:cause {:optional true} any?]
              [:data {:optional true} any?]
              [:phase {:optional true} any?]]]]]])

(def ShellHistory
  [:vector string?])

(def ShellHistoryPosition
  [:or pos-int? zero?])

(def ShellLanguage
  [:map {:closed true}
   [:status {:optional true} LoadingState]
   [:history {:max config/max-shell-history
              :default [""]} ShellHistory]
   [:history-pos {:default 0} ShellHistoryPosition]
   [:items {:max config/max-shell-history
            :default []} [:vector ShellItem]]])

(def ShellCompletionPosition
  [:or pos-int? zero?])

(def ShellCompletionWord
  [:tuple
   [string? {:title "namespaced symbol"}]
   [string? {:title "name"}]])

(def ShellCompletion
  [:map {:closed true}
   [:active boolean?]
   [:show-all boolean?]
   [:initial-text string?]
   [:pos ShellCompletionPosition]
   [:words {:default []} ShellCompletionWord]])

(def Shell
  [:map {:closed true}
   [:verbose {:default false} boolean?]
   [:languages {:default {}} [:map-of ShellLanguageId ShellLanguage]]
   [:active-language {:default :cljs} keyword?]
   [:completion {:optional true} ShellCompletion]])

(def default-lang (m/decode ShellLanguage
                            {}
                            m.transform/default-value-transformer))
