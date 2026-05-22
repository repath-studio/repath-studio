(ns renderer.timeline.effects
  (:require
   [re-frame.core :as rf]
   [renderer.utils.dom :as utils.dom]))

(defn svg-elements!
  []
  (some-> (utils.dom/get-frame-document)
          (.querySelectorAll "svg")))

(rf/reg-fx
 ::set-current-time
 (fn [t]
   (->> (svg-elements!)
        (map #(.setCurrentTime % t))
        (doall))))

(rf/reg-fx
 ::pause-animations
 (fn []
   (->> (svg-elements!)
        (map #(.pauseAnimations %))
        (doall))))
