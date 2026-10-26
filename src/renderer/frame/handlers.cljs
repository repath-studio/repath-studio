(ns renderer.frame.handlers
  (:require
   [config :as config]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [BBox Vec2 DomRect Viewbox]]
   [renderer.document.db :refer [DocumentId ZoomFactor]]
   [renderer.element.handlers :as element.handlers]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.extra :refer [rpartial]]
   [renderer.utils.math :as utils.math]))

(m/=> viewbox [:function
               [:-> App [:maybe Viewbox]]
               [:-> ZoomFactor Vec2 DomRect Viewbox]])
(defn viewbox
  ([db]
   (let [{:keys [active-document dom-rect]} db
         {:keys [zoom pan]} (get-in db [:documents active-document])]
     (some->> dom-rect
              (viewbox zoom pan))))
  ([zoom pan dom-rect]
   (let [{:keys [width height]} dom-rect]
     (->> (utils.math/v-div [width height] zoom)
          (into pan)))))

(m/=> viewbox->bounds [:-> Viewbox BBox])
(defn viewbox->bounds
  [[x y w h]]
  (let [[x2 y2] (utils.math/v-add [w h] [x y])]
    [x y x2 y2]))

(m/=> pan-by [:function
              [:-> App Vec2 App]
              [:-> App DocumentId Vec2 App]])
(defn pan-by
  ([db offset]
   (pan-by db (:active-document db) offset))
  ([db id offset]
   (let [zoom (get-in db [:documents id :zoom])]
     (update-in db [:documents id :pan]
                utils.math/v-add (utils.math/v-div offset zoom)))))

(m/=> recenter-to-dom-rect [:-> App DomRect App])
(defn recenter-to-dom-rect
  [db rect]
  (let [{:keys [document-tabs dom-rect]} db]
    (if-not dom-rect
      db
      (let [{:keys [width height]} (merge-with - dom-rect rect)
            offset (utils.math/v-div [width height] 2)]
        (reduce (rpartial pan-by offset) db document-tabs)))))

(m/=> zoom-at-position [:-> App number? Vec2 App])
(defn zoom-at-position
  [db factor pos]
  (let [active-document (:active-document db)
        {:keys [zoom pan]} (get-in db [:documents active-document])
        updated-zoom (-> (* zoom factor)
                         (utils.math/clamp config/min-zoom config/max-zoom))
        updated-factor (/ updated-zoom zoom)
        delta (utils.math/v-sub (utils.math/v-div pos updated-factor) pos)
        updated-pan (utils.math/v-sub (utils.math/v-div pan updated-factor)
                                      delta)]
    (-> db
        (assoc-in [:documents active-document :zoom] updated-zoom)
        (assoc-in [:documents active-document :pan] updated-pan))))

(m/=> zoom-at-pointer [:-> App number? App])
(defn zoom-at-pointer
  [db factor]
  (zoom-at-position db factor (:local-pointer-pos db)))

(m/=> zoom-in-place [:-> App number? App])
(defn zoom-in-place
  [db factor]
  (let [{:keys [active-document dom-rect]} db
        {:keys [zoom pan]} (get-in db [:documents active-document])
        {:keys [width height]} dom-rect]
    (cond-> db
      active-document
      (zoom-at-position factor
                        (utils.math/v-add pan
                                          (utils.math/v-div [width height]
                                                            2 zoom))))))

(m/=> pan-to-bbox [:-> App BBox App])
(defn pan-to-bbox
  [db bbox]
  (let [{:keys [active-document dom-rect]} db
        zoom (get-in db [:documents active-document :zoom])
        rect-dimensions [(:width dom-rect) (:height dom-rect)]
        [min-x min-y] bbox
        pan (-> (utils.bounds/->dimensions bbox)
                (utils.math/v-sub (utils.math/v-div rect-dimensions zoom))
                (utils.math/v-div 2)
                (utils.math/v-add [min-x min-y]))]
    (assoc-in db [:documents active-document :pan] pan)))

(m/=> content-bbox [:-> App [:maybe BBox]])
(defn content-bbox
  [db]
  (some-> (element.handlers/root-children db)
          (utils.element/united-bbox)))

(m/=> focus [:function
             [:-> App App]
             [:-> App [:enum :original :fit :fill] App]
             [:-> App [:enum :original :fit :fill] BBox App]])
(defn focus
  ([db]
   (cond-> db
     (:active-document db)
     (focus :original (content-bbox db))))
  ([db focus-type]
   (cond-> db
     (:active-document db)
     (focus
      focus-type
      (or (element.handlers/bbox db)
          (content-bbox db)))))
  ([db focus-type bbox]
   (let [[w h] (utils.bounds/->dimensions bbox)
         {:keys [active-document dom-rect]} db
         width-ratio (/ (:width dom-rect) w)
         height-ratio (/ (:height dom-rect) h)]
     (-> db
         (assoc-in [:documents active-document :zoom]
                   (case focus-type
                     :original (min (* (min width-ratio height-ratio) 0.9) 1)
                     :fit (min width-ratio height-ratio)
                     :fill (max width-ratio height-ratio)))
         (pan-to-bbox bbox)))))

(m/=> scroll-bounds [:-> BBox Vec2 BBox])
(defn scroll-bounds
  [[min-x min-y max-x max-y] [viewport-width viewport-height]]
  [(- min-x (/ viewport-width 2))
   (- min-y (/ viewport-height 2))
   (+ max-x (/ viewport-width 2))
   (+ max-y (/ viewport-height 2))])

(m/=> clamp-pan [:-> BBox Vec2 number? Vec2 Vec2])
(defn clamp-pan
  [bbox [viewport-width viewport-height] zoom target]
  (let [[cmin-x cmin-y cmax-x cmax-y]
        (scroll-bounds bbox [viewport-width viewport-height])
        [vb-width vb-height] (utils.math/v-div [viewport-width viewport-height]
                                               zoom)]
    (mapv utils.math/clamp target
          [cmin-x cmin-y]
          [(max cmin-x (- cmax-x vb-width))
           (max cmin-y (- cmax-y vb-height))])))

(m/=> pan-by-in-bounds [:-> App Vec2 App])
(defn pan-by-in-bounds
  [db offset]
  (let [id (:active-document db)
        zoom (get-in db [:documents id :zoom])
        target (utils.math/v-add (get-in db [:documents id :pan])
                                 (utils.math/v-div offset zoom))]
    (if-let [clamped (when-let [{:keys [width height]} (:dom-rect db)]
                       (when-let [bbox (content-bbox db)]
                         (clamp-pan bbox [width height] zoom target)))]
      (assoc-in db [:documents id :pan] clamped)
      (pan-by db id offset))))

(m/=> axis-scroll [:-> number? number? number? number? ZoomFactor map?])
(defn axis-scroll
  [content-min content-max viewbox-min viewport-span track-span zoom]
  (let [content-span (- content-max content-min)
        viewbox-max (+ viewbox-min (/ viewport-span zoom))
        visible-min (max content-min viewbox-min)
        visible-max (min content-max viewbox-max)
        scale (/ track-span content-span)
        min-thumb-size 24
        size (max (* (- visible-max visible-min) scale) min-thumb-size)
        offset (-> (- visible-min content-min)
                   (* scale)
                   (utils.math/clamp 0 (- track-span size)))]
    {:size size
     :offset offset
     :ratio scale}))
