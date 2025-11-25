(ns renderer.frame.handlers
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [BBox Vec2]]
   [renderer.document.db :as document.db :refer [ZoomFactor]]
   [renderer.element.handlers :as element.handlers]
   [renderer.frame.db :refer [DomRect Viewbox FocusType]]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
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
     (->> (matrix/div [width height] zoom)
          (into pan)))))

(m/=> pan-by [:function
              [:-> App Vec2 App]
              [:-> App Vec2 uuid? App]])
(defn pan-by
  ([db offset]
   (pan-by db offset (:active-document db)))
  ([db offset id]
   (let [zoom (get-in db [:documents id :zoom])]
     (update-in db [:documents id :pan] matrix/add (matrix/div offset zoom)))))

(m/=> recenter-to-dom-rect [:-> App DomRect App])
(defn recenter-to-dom-rect
  [db updated-dom-rect]
  (let [{:keys [document-tabs dom-rect]} db
        delta-rect (merge-with - dom-rect updated-dom-rect)
        offset (matrix/div [(:width delta-rect) (:height delta-rect)] 2)]
    (if-not (-> db :window :focused)
      db
      (reduce (fn [db id] (pan-by db offset id)) db document-tabs))))

(m/=> zoom-at-position [:-> App number? Vec2 App])
(defn zoom-at-position
  [db factor pos]
  (let [active-document (:active-document db)
        {:keys [zoom pan]} (get-in db [:documents active-document])
        updated-zoom (-> (* zoom factor)
                         (utils.math/clamp document.db/min-zoom
                                           document.db/max-zoom))
        updated-factor (/ updated-zoom zoom)
        updated-pan (matrix/sub (matrix/div pan updated-factor)
                                (matrix/sub (matrix/div pos updated-factor)
                                            pos))]
    (-> db
        (assoc-in [:documents active-document :zoom] updated-zoom)
        (assoc-in [:documents active-document :pan] updated-pan))))

(m/=> adjusted-pointer-pos [:-> App Vec2 Vec2])
(defn adjusted-pointer-pos
  [db pos]
  (let [{:keys [zoom pan]} (get-in db [:documents (:active-document db)])]
    (-> pos
        (matrix/div zoom)
        (matrix/add pan))))

(m/=> zoom-at-pointer [:-> App number? App])
(defn zoom-at-pointer
  [db factor]
  (zoom-at-position db factor (:adjusted-pointer-pos db)))

(m/=> zoom-in-place [:-> App number? App])
(defn zoom-in-place
  [db factor]
  (let [{:keys [active-document dom-rect]} db
        {:keys [zoom pan]} (get-in db [:documents active-document])
        {:keys [width height]} dom-rect
        position (matrix/add pan (matrix/div [width height] 2 zoom))]
    (zoom-at-position db factor position)))

(m/=> pan-to-bbox [:-> App BBox App])
(defn pan-to-bbox
  [db bbox]
  (let [{:keys [active-document dom-rect]} db
        zoom (get-in db [:documents active-document :zoom])
        rect-dimensions [(:width dom-rect) (:height dom-rect)]
        [min-x min-y] bbox
        pan (-> (utils.bounds/->dimensions bbox)
                (matrix/sub (matrix/div rect-dimensions zoom))
                (matrix/div 2)
                (matrix/add [min-x min-y]))]
    (assoc-in db [:documents active-document :pan] pan)))

(m/=> focus-bbox [:function
                  [:-> App FocusType App]
                  [:-> App FocusType BBox App]])
(defn focus-bbox
  ([db focus-type]
   (cond-> db
     (:active-document db)
     (focus-bbox
      focus-type
      (or (element.handlers/bbox db)
          (utils.element/united-bbox (element.handlers/root-children db))))))
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
