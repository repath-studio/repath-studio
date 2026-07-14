(ns renderer.shell.db
  (:require
   [renderer.db :refer [LoadingState]]
   [renderer.hierarchy :as hierarchy]
   [renderer.shell.hierarchy :as shell.hierarchy]))

(defn shell-language?
  [k]
  (contains? (descendants @hierarchy/hierarchy ::shell.hierarchy/language) k))

(def ShellLanguage
  [:fn {:error/fn (fn [{:keys [value]} _]
                    (str value ", is not a supported language"))}
   shell-language?])

(def Shell
  [:map {:closed true}
   [:verbose {:default false} boolean?]
   [:language-status {:default {}} [:map-of ShellLanguage LoadingState]]
   [:active-language {:default :cljs} ShellLanguage]])
