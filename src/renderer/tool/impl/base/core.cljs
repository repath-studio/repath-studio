(ns renderer.tool.impl.base.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.tool.impl.base.edit]
   [renderer.tool.impl.base.pan]
   [renderer.tool.impl.base.transform.core]
   [renderer.tool.impl.base.zoom]))

(rf/dispatch [::action.events/register-action-group
              {:id :tools/transform
               :label [::transform "Transform"]
               :actions [:tool/transform
                         :tool/edit
                         :tool/pan
                         :tool/zoom]}])
