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
              {:id :view/toggle-fullscreen
               :label [::toggle-fullscreen "Toggle fullscreen"]
               :icon "arrow-minimize"
               :event [::window.events/toggle-fullscreen]
               :shortcuts [{:keyCode (utils.key/codes "F11")}]
               :available [::app.subs/not-mobile?]
               :active [::window.subs/fullscreen?]}])

(rf/dispatch [::action.events/register-action
              {:id :window/close
               :label [::exit "Exit"]
               :icon "exit"
               :event [::window.events/close]
               :shortcuts [{:keyCode (utils.key/codes "Q")
                            :ctrlKey true}]}])
