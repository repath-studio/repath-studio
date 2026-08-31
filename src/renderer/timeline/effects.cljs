(ns renderer.timeline.effects
  (:require
   [re-frame.core :as rf]
   [renderer.utils.dom :as utils.dom]))

(defn get-svg-elements
  []
  (some-> (utils.dom/get-frame-document)
          (.querySelectorAll "svg")))

(rf/reg-fx
 ::set-current-time
 (fn [t]
   (doseq [el (get-svg-elements)]
     (.setCurrentTime el t))))

(rf/reg-fx
 ::pause-animations
 (fn []
   (doseq [el (get-svg-elements)]
     (.pauseAnimations el))))
