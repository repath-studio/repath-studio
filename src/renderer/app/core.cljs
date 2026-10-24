(ns renderer.app.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.app.effects]
   [renderer.app.events :as app.events]
   [renderer.app.subs :as app.subs]
   [renderer.utils.key :as utils.key]))

(rf/dispatch [::action.events/register-action
              {:id :app/toggle-help-bar
               :label [::help-bar "Help bar"]
               :active [::app.subs/help-bar]
               :event [::app.events/toggle-help-bar]}])

(rf/dispatch [::action.events/register-action
              {:id :app/toggle-debug-info
               :label [::debug-info "Debug info"]
               :event [::app.events/toggle-debug-info]
               :active [::app.subs/debug-info]
               :shortcuts {"All" [{:keyCode (utils.key/codes "D")
                                   :ctrlKey true
                                   :shiftKey true}]
                           "Mac" [{:keyCode (utils.key/codes "D")
                                   :metaKey true
                                   :shiftKey true}]}}])

(rf/dispatch [::action.events/register-action
              {:id :app/auto-update
               :label [::check-for-updates "Check for updates automatically"]
               :event [::app.events/toggle-auto-update]
               :available [::app.subs/desktop?]
               :active [::app.subs/auto-update?]}])
