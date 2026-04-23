(ns renderer.tool.impl.base.transform.clone
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [Orientation]]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.input.db :refer [PointerEvent]]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.transform.select :as transform.select]
   [renderer.tool.impl.base.transform.translate :as transform.translate]
   [renderer.views :as views]))

(defmethod tool.hierarchy/help [:transform :clone]
  []
  (i18n.views/t [::clone
                 [:div "Hold %1 to restrict direction. or release %2 to move"]]
                [[views/kbd "Ctrl"]
                 [views/kbd "Alt"]]))

(m/=> on-drag [:-> App Orientation PointerEvent App])
(defn on-drag
  [db axis e]
  (let [{:keys [shift-key]} e
        delta (tool.handlers/pointer-delta db)]
    (-> db
        (history.handlers/reset-state)
        (transform.select/select-element shift-key)
        (element.handlers/duplicate)
        (transform.translate/translate delta axis)
        (snap.handlers/snap-with transform.translate/translate axis)
        (tool.handlers/set-cursor "copy"))))
