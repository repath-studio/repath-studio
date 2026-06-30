(ns renderer.tool.impl.extension.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.tool.impl.extension.blob]))

(rf/dispatch [::action.events/register-action-group
              {:id :tools/extensions
               :icon "extension"
               :label [::extensions "Extensions"]
               :actions [:tool/blob]}])
