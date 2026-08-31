(ns renderer.window.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.app.subs :as-alias app.subs]
   [renderer.utils.key :as utils.key]
   [renderer.window.effects]
   [renderer.window.events :as window.events]
   [renderer.window.subs :as window.subs]))

(rf/dispatch [::action.events/register-action
              {:id :window/toggle-fullscreen
               :label [::toggle-fullscreen "Toggle fullscreen"]
               :icon "arrow-minimize"
               :event [::window.events/toggle-fullscreen]
               :shortcuts [{:keyCode (utils.key/codes "F11")}]
               :available [::app.subs/not-mobile?]
               :active [::window.subs/fullscreen?]}])

(rf/dispatch [::action.events/register-action
              {:id :window/toggle-maximized
               :label [::toggle-maximized "Toggle maximized"]
               :icon "window-restore"
               :event [::window.events/toggle-maximized]
               :available [::app.subs/desktop?]
               :active [::window.subs/maximized?]}])

(rf/dispatch [::action.events/register-action
              {:id :window/minimize
               :label [::minimize "Minimize"]
               :icon "window-minimize"
               :event [::window.events/minimize]
               :available [::app.subs/desktop?]}])

(rf/dispatch [::action.events/register-action
              {:id :window/close
               :label [::exit "Exit"]
               :icon "exit"
               :event [::window.events/close]
               :shortcuts [{:keyCode (utils.key/codes "Q")
                            :ctrlKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :window/toggle-devtools
               :label [::toggle-devtools "Toggle developer tools"]
               :icon "window-restore"
               :event [::window.events/toggle-devtools]
               :available [::app.subs/desktop?]
               :shortcuts [{:keyCode (utils.key/codes "I")
                            :ctrlKey true
                            :shiftKey true}]}])

(rf/dispatch [::action.events/register-action-group
              {:id :window/actions
               :icon "window-maximize"
               :label [::window "Window"]
               :actions [:window/toggle-maximized
                         :window/toggle-fullscreen
                         :window/minimize
                         :window/close
                         :window/toggle-devtools]}])
