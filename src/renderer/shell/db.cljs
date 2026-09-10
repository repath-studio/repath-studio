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

(def ShellInfoItemProtocol
  [:enum :url :command])

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
   [:info
    [:map {:closed true}
     [:type [:= :info]]
     [:value [:or
              string?
              [:sequential [:or
                            string?
                            [:tuple ShellInfoItemProtocol string?]]]]]]]
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

(def ShellCompletionItem
  [:tuple
   [:or nil? string? symbol?]
   [:or nil? string?]])

(def ShellCompletion
  [:map {:closed true}
   [:active {:optional true} boolean?]
   [:show-all {:optional true} boolean?]
   [:initial-text {:optional true} string?]
   [:pos {:optional true} ShellCompletionPosition]
   [:words {:optional true} [:vector ShellCompletionItem]]])

(def Shell
  [:map {:closed true}
   [:verbose {:default false} boolean?]
   [:languages {:default {}} [:map-of ShellLanguageId ShellLanguage]]
   [:active-language {:default :cljs} keyword?]
   [:completion {:optional true} [:maybe ShellCompletion]]])

(def default-lang (m/decode ShellLanguage
                            {}
                            m.transform/default-value-transformer))
