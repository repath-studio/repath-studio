(ns renderer.error.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.error.effects]
   [renderer.error.events :as error.events]
   [renderer.error.subs :as error.subs]))

(rf/dispatch [::action.events/register-action
              {:id :error/toggle-reporting
               :icon "bug"
               :label [::report-errors "Report errors automatically"]
               :active [::error.subs/reporting?]
               :event [::error.events/toggle-reporting]}])
