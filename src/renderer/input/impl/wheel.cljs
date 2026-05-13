(ns renderer.input.impl.wheel
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.effects]
   [renderer.app.handlers :as app.handlers]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.input.db :refer [WheelEvent]]
   [renderer.input.events :as-alias input.events]
   [renderer.input.hierarchy :as input.hierarchy]
   [renderer.snap.handlers :as snap.handlers]))

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

  (rf/dispatch-sync [::input.events/wheel (->clj e)]))

(defmethod input.hierarchy/wheel :default
  [db e]
  (let [{:keys [delta-x delta-y ctrl-key shift-key]} e]
    (-> (if (or ctrl-key shift-key)
          (let [factor (-> (:zoom-sensitivity db)
                           dec (/ 100) inc
                           (Math/pow delta-y))]
            (frame.handlers/zoom-at-pointer db factor))
          (frame.handlers/pan-by db [delta-x delta-y]))
        (snap.handlers/update-viewport-tree)
        (app.handlers/add-fx [::app.effects/persist]))))
