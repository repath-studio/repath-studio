(ns renderer.app.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.app.effects]
   [renderer.app.events :as app.events]
   [renderer.app.subs :as app.subs]
   [renderer.utils.key :as utils.key]))

(rf/dispatch [::action.events/register-action
              {:id :help-view/toggle-help-bar
               :label [::help-bar "Help bar"]
               :icon "info"
               :active [::app.subs/help-bar]
               :event [::app.events/toggle-help-bar]}])

(rf/dispatch [::action.events/register-action
              {:id :help-view/toggle-debug-info
               :label [::debug-info "Debug info"]
               :icon "bug"
               :event [::app.events/toggle-debug-info]
               :active [::app.subs/debug-info]
               :shortcuts [{:keyCode (utils.key/codes "D")
                            :ctrlKey true
                            :shiftKey true}]}])

(rf/dispatch [::action.events/register-action-group
              {:id :help-view
               :icon "toggles"
               :label [::help-view "Help view"]
               :actions [:help-view/toggle-help-bar
                         :help-view/toggle-debug-info]}])
