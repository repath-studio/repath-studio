(ns renderer.reepl.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.reepl.effects]
   [renderer.reepl.events :as reepl.events]
   [renderer.reepl.impl.core]
   [renderer.utils.key :as utils.key]))

(rf/dispatch [::action.events/register-action
              {:id :reepl/focus
               :label [::focus-shell "Focus shell"]
               :icon "shell"
               :event [::reepl.events/focus]
               :shortcuts [{:keyCode (utils.key/codes "SLASH")}]}])
