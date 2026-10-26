(ns renderer.frame.scrollbar-view
  "Custom scrollbar implementation for the main canvas."
  (:require
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.frame.events :as-alias frame.events]
   [renderer.utils.math :as utils.math]))

(def thumb-thickness (/ config/track-thickness 2))

(defn axis-delta
  [orientation delta]
  (if (= orientation :horizontal)
    [delta 0]
    [0 delta]))

(defn pan-by
  [ratio orientation delta-px]
  (rf/dispatch [::frame.events/pan-by
                (axis-delta orientation (/ delta-px ratio))]))

(defn thumb
  [orientation offset size margin]
  [:div
   {:class ["absolute rounded-full bg-backdrop"
            "opacity-50 hover:opacity-80 active:opacity-80"]
    :style (if (= orientation :horizontal)
             {:left offset
              :width size
              :top margin
              :height thumb-thickness}
             {:top offset
              :height size
              :left margin
              :width thumb-thickness})}])

(defn on-pointer-down
  [orientation {:keys [size offset ratio track]} dom-rect]
  (fn [e]
    (.preventDefault e)
    (.setPointerCapture (.-currentTarget e) (.-pointerId e))
    (when (identical? (.-target e) (.-currentTarget e))
      (let [horizontal? (= orientation :horizontal)
            position (if horizontal?
                       (- (.-clientX e) (:left dom-rect))
                       (- (.-clientY e) (:top dom-rect)))
            target-offset (utils.math/clamp (- position (/ size 2))
                                            0
                                            (- track size))]
        (pan-by ratio orientation (- target-offset offset))))))

(defn on-pointer-move
  [orientation {:keys [offset size ratio track]}]
  (fn [e]
    (when (.hasPointerCapture (.-currentTarget e) (.-pointerId e))
      (let [move (if (= orientation :horizontal)
                   (.-movementX e)
                   (.-movementY e))
            at-start? (<= offset 0)
            at-end? (>= offset (- track size))
            skip? (or (zero? move)
                      (and at-start? (neg? move))
                      (and at-end? (pos? move)))]
        (when-not skip?
          (pan-by ratio orientation move))))))

(defn root
  [orientation options]
  (let [horizontal? (= orientation :horizontal)
        margin (/ (- config/track-thickness thumb-thickness) 2)
        {:keys [size offset]} options
        dom-rect @(rf/subscribe [::app.subs/dom-rect])]
    [:div.absolute
     {:class (if horizontal?
               "bottom-0 left-0"
               "top-0 right-0")
      :style (if horizontal?
               {:height config/track-thickness
                :right config/track-thickness}
               {:width config/track-thickness
                :bottom config/track-thickness})
      :on-pointer-down (on-pointer-down orientation options dom-rect)
      :on-pointer-move (on-pointer-move orientation options)}
     [thumb orientation offset size margin]]))
