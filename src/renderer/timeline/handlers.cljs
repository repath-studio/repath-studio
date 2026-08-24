(ns renderer.timeline.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.element.db :refer [ElementId]]
   [renderer.element.handlers :as element.handlers]
   [renderer.utils.attribute :as utils.attribute]))

(m/=> update-action [:-> App ElementId number? number? App])
(defn update-action
  [db id start end]
  (cond-> db
    :always
    (-> (element.handlers/toggle-selection id false)
        (element.handlers/set-attr :begin (utils.attribute/->fixed start))
        (element.handlers/set-attr :end (utils.attribute/->fixed end)))

    (-> db :timeline :fit-duration)
    (element.handlers/set-attr :dur (utils.attribute/->fixed (- end start)))))
