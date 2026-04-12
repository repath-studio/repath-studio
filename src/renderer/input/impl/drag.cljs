(ns renderer.input.impl.drag
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.db :refer [JS_Object]]
   [renderer.input.db :refer [DragEvent]]
   [renderer.input.events :as-alias input.events]))

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
