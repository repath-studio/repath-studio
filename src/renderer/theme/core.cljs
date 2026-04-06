(ns renderer.theme.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.theme.effects]
   [renderer.theme.events :as theme.events]
   [renderer.theme.subs :as theme.subs]))

(rf/dispatch [::action.events/register-action
              {:id :theme/set-dark-mode
               :label [::dark "Dark"]
               :icon "dark"
               :event [::theme.events/set-mode :dark]
               :active [::theme.subs/selected-mode? :dark]}])

(rf/dispatch [::action.events/register-action
              {:id :theme/set-light-mode
               :label [::light "Light"]
               :icon "light"
               :event [::theme.events/set-mode :light]
               :active [::theme.subs/selected-mode? :light]}])

(rf/dispatch [::action.events/register-action
              {:id :theme/set-system-mode
               :label [::system "System"]
               :icon "system"
               :event [::theme.events/set-mode :system]
               :active [::theme.subs/selected-mode? :system]}])
