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

(defn segment-bounds
  [match snapped-base]
  (let [{:keys [axis point]} match
        x-axis? (= axis :x)]
    (when (and point (-> match :base-point meta :label))
      (let [coord (if x-axis? second first)
            from (coord point)
            to (coord snapped-base)]
        [(min from to) (max from to)]))))

(defn axis-line
  [match [line-min line-max] zoom]
  (let [{:keys [axis value]} match
        x-axis? (= axis :x)]
    [:line (cond-> {:shape-rendering "crispEdges"
                    :pointer-events "none"
                    :stroke "var(--accent)"
                    :stroke-width (/ 1 zoom)
                    :stroke-dasharray (/ 5 zoom)}
             x-axis?
             (assoc :x1 value
                    :y1 line-min
                    :x2 value
                    :y2 line-max)

             (not x-axis?)
             (assoc :x1 line-min
                    :y1 value
                    :x2 line-max
                    :y2 value))]))

(defn labeled-point
  [at text margin]
  (let [[x y] (utils.math/v-add at [margin margin])]
    [:<>
     [utils.svg/times at]
     [utils.svg/label text
      {:x x
       :y y
       :text-anchor "start"
       :font-family "var(--font-sans)"}]]))

(defn axis-indicator
  [match other-delta]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        margin (/ 15 zoom)
        {:keys [axis point label delta]} match
        base-point (:base-point match)
        base-label (-> base-point meta :label)
        x-axis? (= axis :x)
        snapped-base (utils.math/v-add
                      base-point
                      (if x-axis? [delta other-delta] [other-delta delta]))
        segment (segment-bounds match snapped-base)]
    [:<>
     (when segment
       [axis-line match segment zoom])

     (when point
       (if label
         [labeled-point point (i18n.views/t label) margin]
         [utils.svg/times point]))

     (when base-label
       [labeled-point snapped-base (i18n.views/t base-label) margin])]))
