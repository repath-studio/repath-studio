(ns renderer.tool.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.tool.events :as tool.events]
   [renderer.tool.impl.core]
   [renderer.tool.subs]
   [renderer.utils.key :as utils.key]))

(rf/dispatch [::action.events/register-action
              {:id :tool/cancel
               :label [::cancel "Cancel"]
               :icon "window-close"
               :event [::tool.events/cancel]
               :shortcuts [{:keyCode (utils.key/codes "ESC")}]}])
