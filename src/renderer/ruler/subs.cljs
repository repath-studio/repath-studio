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
 (fn [zoom _]
   (ruler.handlers/step conf/grid-target-px zoom)))

(rf/reg-sub
 ::steps-coll
 :<- [::step]
 :<- [::frame.subs/viewbox]
 (fn [[step viewbox] [_ orientation]]
   (ruler.handlers/steps-coll step viewbox orientation)))

(rf/reg-sub
 ::render-subgrid?
 :<- [::step]
 (fn [step _]
   (> step (ruler.handlers/step conf/grid-target-px conf/max-zoom))))

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
