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
   (let [{:keys [width height]} dom-rect
         [content-min-x content-min-y content-max-x content-max-y]
         (frame.handlers/scroll-bounds bbox [width height])]
     (if (= orientation :horizontal)
       (frame.handlers/axis-scroll content-min-x
                                   content-max-x
                                   viewbox-min-x
                                   (/ width zoom)
                                   (- width config/track-thickness))
       (frame.handlers/axis-scroll content-min-y content-max-y
                                   viewbox-min-y
                                   (/ height zoom)
                                   (- height config/track-thickness))))))
