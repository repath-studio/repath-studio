(ns renderer.tool.impl.base.transform.idle
  (:require [renderer.i18n.views :as i18n.views]
            [renderer.tool.hierarchy :as tool.hierarchy]
            [renderer.views :as views]))

(defmethod tool.hierarchy/help [:transform :idle]
  []
  [:<>
   (i18n.views/t [::idle-click
                  [:div "Click to select an element or drag to select by
                         area."]])
   (i18n.views/t [::idle-hold
                  [:div "Hold %1 to add or remove elements to selection."]]
                 [[views/kbd "⇧"]])])
