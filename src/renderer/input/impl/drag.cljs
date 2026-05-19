(ns renderer.input.impl.drag
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.handlers :as app.handlers]
   [renderer.db :refer [JS_Object]]
   [renderer.input.db :refer [DragEvent]]
   [renderer.input.effects :as-alias input.effects]
   [renderer.input.events :as-alias input.events]
   [renderer.input.handlers :as input.handlers]
   [renderer.input.hierarchy :as input.hierarchy]))

(m/=> ->clj [:-> JS_Object DragEvent])
(defn ->clj
  "https://developer.mozilla.org/en-US/docs/Web/API/DragEvent"
  [^js/DragEvent e]
  {:type (.-type e)
   :pointer-pos [(.-pageX e) (.-pageY e)]
   :data-transfer (.-dataTransfer e)})

(m/=> handler! [:-> JS_Object nil?])
(defn handler!
  [^js/DragEvent e]
  (.stopPropagation e)
  (.preventDefault e)

  (rf/dispatch-sync [::input.events/drag (->clj e)]))

(defmethod input.hierarchy/drag "drop"
  [db e]
  (let [{:keys [data-transfer pointer-pos]} e
        position (input.handlers/adjusted-pos db pointer-pos)]
    (app.handlers/add-fx db [::input.effects/drop [position data-transfer]])))
