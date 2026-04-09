(ns renderer.dialog.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.dialog.events :as dialog.events]
   [renderer.dialog.subs]
   [renderer.utils.key :as utils.key]))

(rf/dispatch [::action.events/register-action
              {:id :dialog/about
               :label [::about "About"]
               :icon "info"
               :event [::dialog.events/show-about]}])

(def cmdk-title [::command-panel "Command panel"])

(rf/dispatch [::action.events/register-action
              {:id :dialog/command-panel
               :label cmdk-title
               :icon "command"
               :event [::dialog.events/show-cmdk cmdk-title]
               :shortcuts [{:keyCode (utils.key/codes "F1")}
                           {:keyCode (utils.key/codes "K")
                            :ctrlKey true}]}])
