(ns renderer.tool.impl.misc.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.tool.impl.misc.dropper]
   [renderer.tool.impl.misc.fill]
   [renderer.tool.impl.misc.measure]))

(rf/dispatch [::action.events/register-action-group
              {:id :tools/misc
               :label [::misc "Miscallaneous"]
               :actions [:tool/fill
                         :tool/measure
                         :tool/eye-dropper]}])
