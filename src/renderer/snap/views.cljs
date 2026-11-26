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

(defn axis-indicator
  [match other-delta viewbox-bounds]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        margin (/ 15 zoom)
        [min-x min-y max-x max-y] viewbox-bounds
        {:keys [axis value point label delta]} match
        base-point (:base-point match)
        base-label (-> base-point meta :label)
        x-axis? (= axis :x)
        snapped-base (utils.math/v-add
                      base-point
                      (if x-axis? [delta other-delta] [other-delta delta]))
        label-at (fn [text at]
                   (let [[x y] (utils.math/v-add at [margin margin])]
                     [utils.svg/label text
                      {:x x
                       :y y
                       :text-anchor "start"
                       :font-family "var(--font-sans)"}]))]
    [:<>
     [:line (cond-> {:shape-rendering "crispEdges"
                     :pointer-events "none"
                     :stroke "var(--accent)"
                     :stroke-width (/ 1 zoom)
                     :stroke-dasharray (/ 5 zoom)}
              x-axis?
              (assoc :x1 value
                     :y1 min-y
                     :x2 value
                     :y2 max-y)

              (not x-axis?)
              (assoc :x1 min-x
                     :y1 value
                     :x2 max-x
                     :y2 value))]

     (when point
       [:<>
        [utils.svg/times point]
        (when label
          (label-at (i18n.views/t label) point))])

     (when base-label
       [:<>
        [utils.svg/times snapped-base]
        (label-at (i18n.views/t base-label) snapped-base)])]))
