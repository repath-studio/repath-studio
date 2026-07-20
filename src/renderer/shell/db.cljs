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

(def ShellItem
  [:multi {:dispatch :type}
   [:input
    [:map {:closed true}
     [:type [:= :input]]
     [:current-ns any?]
     [:num int?]
     [:value string?]]]
   [:output
    [:map {:closed true}
     [:type [:= :output]]
     [:value any?]]]
   [:error
    [:map {:closed true}
     [:type [:= :error]]
     [:value [:map
              [:via {:optional true}
               [:vector [:map {:optional true}
                         [:type {:optional true} any?]
                         [:message {:optional true} any?]
                         [:data {:optional true} any?]
                         [:at {:optional true} any?]]]]
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
   [:history {:default [""]} ShellHistory]
   [:history-pos {:default 0} ShellHistoryPosition]
   [:items {:default []} [:vector ShellItem]]])

(def Shell
  [:map {:closed true}
   [:verbose {:default false} boolean?]
   [:languages {:default {}} [:map-of ShellLanguageId ShellLanguage]]
   [:active-language {:default :cljs} keyword?]])

(def default-lang (m/decode ShellLanguage
                            {}
                            m.transform/default-value-transformer))
