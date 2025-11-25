(ns renderer.event.impl.wheel
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.event.db :refer [WheelEvent]]
   [renderer.event.events :as-alias event.events]))

(m/=> ->clj [:-> any? WheelEvent])
(defn ->clj
  "https://developer.mozilla.org/en-US/docs/Web/API/WheelEvent"
  [^js/WheelEvent e]
  {:target (.-target e)
   :type (.-type e)
   :pointer-pos [(.-pageX e) (.-pageY e)]
   :delta-x (.-deltaX e)
   :delta-y (.-deltaY e)
   :delta-z (.-deltaZ e)
   :alt-key (.-altKey e)
   :ctrl-key (.-ctrlKey e)
   :meta-key (.-metaKey e)
   :shift-key (.-shiftKey e)})

(m/=> handler! [:-> any? nil?])
(defn handler!
  [^js/WheelEvent e]
  (.stopPropagation e)
  (.preventDefault e)

  (rf/dispatch-sync [::event.events/wheel (->clj e)]))
