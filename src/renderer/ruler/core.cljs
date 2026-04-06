(ns renderer.ruler.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.ruler.events :as ruler.events]
   [renderer.ruler.subs :as ruler.subs]
   [renderer.utils.key :as utils.key]))

(rf/dispatch [::action.events/register-action
              {:id :view/toggle-rulers
               :label [::rulers "Rulers"]
               :icon "ruler-combined"
               :event [::ruler.events/toggle-visible]
               :active [::ruler.subs/visible?]
               :shortcuts [{:keyCode (utils.key/codes "R")
                            :ctrlKey true}]}])
