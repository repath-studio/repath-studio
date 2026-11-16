(ns renderer.snap.views
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.utils.math :as utils.math]
   [renderer.utils.svg :as utils.svg]))

(defn canvas-label
  [nearest-neighbor]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        margin (/ 15 zoom)
        point-label (-> nearest-neighbor meta :label)
        base-label (-> nearest-neighbor :base-point meta :label)
        point (:point nearest-neighbor)
        [x y] (utils.math/v-add point margin)
        label (->> [base-label point-label]
                   (remove nil?)
                   (map i18n.views/t)
                   (string/join (i18n.views/t [::to " to "])))]
    [:<>
     [utils.svg/times point]
     (when (not-empty label)
       [utils.svg/label label {:x x
                               :y y
                               :text-anchor "start"
                               :font-family "var(--font-sans)"}])]))
