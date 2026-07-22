(ns renderer.shell.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.shell.effects]
   [renderer.shell.events :as shell.events]
   [renderer.shell.impl.core]
   [renderer.shell.subs :as shell.subs]
   [renderer.utils.key :as utils.key]))

(rf/dispatch [::action.events/register-action
              {:id :shell/focus
               :label [::focus-shell "Focus shell"]
               :icon "shell"
               :event [::shell.events/focus]
               :shortcuts [{:keyCode (utils.key/codes "SLASH")}]}])

(rf/dispatch [::action.events/register-action
              {:id :shell/clear-output
               :label [::clear-output "Clear output"]
               :icon "delete"
               :event [::shell.events/clear-items]
               :enabled [::shell.subs/some-items?]}])

(rf/dispatch [::action.events/register-action
              {:id :shell/toggle-verbose
               :label [::verbose-output "Verbose output"]
               :icon "eye"
               :event [::shell.events/toggle-verbose]
               :active [::shell.subs/verbose?]}])
