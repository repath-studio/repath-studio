(ns renderer.frame.subs
  (:require
   [clojure.string :as string]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.frame.handlers :as frame.handlers]))

(rf/reg-sub
 ::viewbox
 :<- [::document.subs/zoom]
 :<- [::document.subs/pan]
 :<- [::app.subs/dom-rect]
 :-> (partial apply frame.handlers/viewbox))

(rf/reg-sub
 ::viewbox-attr
 :<- [::viewbox]
 :-> (partial string/join " "))

(rf/reg-sub
 ::viewbox-bounds
 :<- [::viewbox]
 :-> frame.handlers/viewbox->bounds)

(rf/reg-sub
 ::content-bbox
 (fn [db _]
   (frame.handlers/content-bbox db)))

(rf/reg-sub
 ::scrollbar
 :<- [::content-bbox]
 :<- [::viewbox-bounds]
 :<- [::document.subs/zoom]
 :<- [::app.subs/dom-rect]
 (fn [[bbox [viewbox-min-x viewbox-min-y] zoom dom-rect] [_ orientation]]
   (let [[content-min-x content-min-y content-max-x content-max-y] bbox
         {:keys [width height]} dom-rect
         horizontal? (= orientation :horizontal)
         content-min (if horizontal? content-min-x content-min-y)
         content-max (if horizontal? content-max-x content-max-y)
         viewbox-min (if horizontal? viewbox-min-x viewbox-min-y)
         span (/ (if horizontal? width height) zoom)
         track (- (if horizontal? width height)
                  (* config/track-thickness 2))
         scroll (frame.handlers/axis-scroll [content-min content-max]
                                            viewbox-min
                                            span
                                            track)]
     (update scroll :ratio #(/ % zoom)))))
