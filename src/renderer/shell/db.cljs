(ns renderer.shell.db
  (:require
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

(def ShellLanguage
  [:map {:closed true}
   [:status {:optional true} LoadingState]])

(def Shell
  [:map {:closed true}
   [:verbose {:default false} boolean?]
   [:languages {:default {}} [:map-of ShellLanguageId ShellLanguage]]
   [:active-language {:default :cljs} keyword?]])
