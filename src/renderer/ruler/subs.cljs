(ns renderer.ruler.subs
  (:require
   [config :as conf]
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.subs]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.ruler.handlers :as ruler.handlers]))

(rf/reg-sub
 ::step
 :<- [::document.subs/zoom]
 :<- [::document.subs/grid-step]
 (fn [[zoom grid-step] [_ orientation]]
   (->> (ruler.handlers/axis-min-step grid-step orientation)
        (ruler.handlers/step conf/grid-target-px zoom))))

(rf/reg-sub
 ::steps-coll
 :<- [::step]
 :<- [::frame.subs/viewbox]
 (fn [[step viewbox] [_ orientation]]
   (ruler.handlers/steps-coll step viewbox orientation)))

(rf/reg-sub
 ::render-subgrid?
 :<- [::step]
 :<- [::document.subs/grid-step]
 (fn [[step grid-step] [_ orientation]]
   (let [min-step (ruler.handlers/axis-min-step grid-step orientation)]
     (> step (ruler.handlers/step conf/grid-target-px
                                  conf/max-zoom
                                  min-step)))))

(rf/reg-sub
 ::px-per-step
 :<- [::step]
 :<- [::document.subs/zoom]
 (fn [[step zoom] _]
   (* step zoom)))

(rf/reg-sub
 ::subgrid-opacity
 :<- [::px-per-step]
 (fn [px-per-step _]
   (let [min-opacity 0.2
         max-opacity 0.8
         closeness (Math/log10 (/ px-per-step conf/grid-target-px))]
     (+ min-opacity (* closeness (- max-opacity min-opacity))))))
