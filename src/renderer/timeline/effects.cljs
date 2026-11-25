(ns renderer.timeline.effects
  (:require
   [re-frame.core :as rf]
   [renderer.utils.clock :as utils.clock]
   [renderer.utils.dom :as utils.dom]))

(def resolved-begins (js/WeakMap.))

(defn resolve-animations!
  "Animation instances are resolved when the document timeline crosses their
   boundaries, not at element insertion or edit attribute time. A newly inserted
   or edited animation element stays anchored to its old start until a seek
   crosses its begin. Seeking each svg momentarily past the begin of an
   animation whose begin has not been resolved yet, and back, within one tick so
   no frame shows the intermediate state, forces the instance to resolve from
   its attributes."
  [svgs t]
  (doseq [anim (utils.dom/get-animation-elements)
          :let [raw (.getAttribute anim "begin")
                seconds (utils.clock/->seconds raw)]
          :when (and seconds (not= raw (.get resolved-begins anim)))]
    (doseq [svg svgs]
      (.setCurrentTime svg (+ seconds 0.001))
      (.setCurrentTime svg t))
    (.set resolved-begins anim raw)))

(rf/reg-fx
 ::set-current-time
 (fn [t]
   (let [svgs (utils.dom/get-svg-elements)]
     (doseq [svg svgs]
       (.setCurrentTime svg t))
     (resolve-animations! svgs t))))

(rf/reg-fx
 ::pause-animations
 (fn []
   (doseq [el (utils.dom/get-svg-elements)]
     (.pauseAnimations el))))
