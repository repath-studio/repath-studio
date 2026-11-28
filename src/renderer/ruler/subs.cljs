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
   (ruler.handlers/current-step zoom grid-step orientation)))

(rf/reg-sub
 ::steps-coll
 :<- [::document.subs/zoom]
 :<- [::document.subs/grid-step]
 :<- [::frame.subs/viewbox]
 (fn [[zoom grid-step viewbox] [_ orientation]]
   (-> (ruler.handlers/current-step zoom grid-step orientation)
       (ruler.handlers/steps-coll viewbox orientation))))

(rf/reg-sub
 ::render-subgrid?
 :<- [::document.subs/zoom]
 :<- [::document.subs/grid-step]
 (fn [[zoom grid-step] [_ orientation]]
   (ruler.handlers/render-subgrid? zoom grid-step orientation)))

(rf/reg-sub
 ::subgrid-opacity
 :<- [::document.subs/zoom]
 :<- [::document.subs/grid-step]
 (fn [[zoom grid-step] [_ orientation]]
   (let [min-opacity 0.2
         max-opacity 0.8
         step (ruler.handlers/current-step zoom grid-step orientation)
         closeness (Math/log10 (/ (* step zoom) conf/grid-target-px))]
     (+ min-opacity (* closeness (- max-opacity min-opacity))))))
