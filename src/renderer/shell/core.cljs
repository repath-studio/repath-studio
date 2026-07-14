(ns renderer.shell.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.shell.effects]
   [renderer.shell.events :as shell.events]
   [renderer.shell.impl.core]
   [renderer.shell.subs]
   [renderer.utils.key :as utils.key]))

(rf/dispatch [::action.events/register-action
              {:id :reepl/focus
               :label [::focus-shell "Focus shell"]
               :icon "shell"
               :event [::shell.events/focus]
               :shortcuts [{:keyCode (utils.key/codes "SLASH")}]}])
