(ns renderer.frame.scrollbar-view
  "Custom scrollbar implementation for the main canvas."
  (:require
   [config :as config]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
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
   {:class ["absolute rounded-full transition-opacity"
            "opacity-50 hover:opacity-80 active:opacity-80"]
    :style (if (= orientation :horizontal)
             {:left (+ offset config/track-thickness)
              :width size
              :top 0
              :padding margin
              :height "100%"}
             {:top (+ offset config/track-thickness)
              :height size
              :left 0
              :padding margin
              :width "100%"})}
   [:div {:class "rounded-full bg-backdrop h-full w-full"}]])

(defn on-pointer-down
  [orientation {:keys [size offset ratio track]} dom-rect]
  (fn [e]
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

(defn prevent-resize
  "Prevent pointer events on overlapping panel separators that handle pointer
   events on document level."
  [e]
  (when (.closest (.-target e) "[data-scrollbar]")
    (.preventDefault e)))

(defn root
  [_orientation _options]
  (reagent/create-class
   {:component-did-mount
    #(doseq [event-type #js ["pointerdown" "pointermove" "pointerup"]]
       (.addEventListener js/window event-type prevent-resize true))

    :component-will-unmount
    #(doseq [event-type #js ["pointerdown" "pointermove" "pointerup"]]
       (.removeEventListener js/window event-type prevent-resize true))

    :reagent-render
    (fn [orientation options]
      (let [margin (/ (- config/track-thickness thumb-thickness) 2)
            {:keys [size offset]} options
            dom-rect @(rf/subscribe [::app.subs/dom-rect])]
        [:div.absolute.transition-colors.hover:bg-white.hover:cursor-default!
         {:data-scrollbar true
          :style (if (= orientation :horizontal)
                   {:bottom 0
                    :left 0
                    :height config/track-thickness
                    :right 0}
                   {:top 0
                    :right 0
                    :width config/track-thickness
                    :bottom 0})
          :on-pointer-down (on-pointer-down orientation options dom-rect)
          :on-pointer-move (on-pointer-move orientation options)}
         [thumb orientation offset size margin]]))}))
