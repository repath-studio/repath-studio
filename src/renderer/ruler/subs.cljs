(ns renderer.ruler.subs
  (:require
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.subs]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.ruler.handlers :as ruler.handlers]))

(rf/reg-sub
 ::step
 :<- [::document.subs/zoom]
 :-> ruler.handlers/step)

(rf/reg-sub
 ::steps-coll
 :<- [::step]
 :<- [::frame.subs/viewbox]
 (fn [[step viewbox] [_ orientation]]
   (ruler.handlers/steps-coll step viewbox orientation)))

(rf/reg-sub
 ::subgrid-opacity
 :<- [::document.subs/zoom]
 (fn [zoom _]
   (let [min-opacity 0.35
         max-opacity 0.8
         proximity (- (Math/log10 (ruler.handlers/step zoom))
                      (Math/log10 (/ 10 zoom)))]
     (+ min-opacity (* proximity (- max-opacity min-opacity))))))
