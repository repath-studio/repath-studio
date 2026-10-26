(ns renderer.frame.scrollbar-view
  "Custom scrollbar implementation for the main canvas."
  (:require
   ["react" :as react]
   [config :as config]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.frame.events :as-alias frame.events]))

(def thumb-thickness (/ config/track-thickness 2))

(defn axis-delta
  [orientation delta]
  (if (= orientation :horizontal)
    [delta 0]
    [0 delta]))

(defn pan
  [ratio delta-px]
  (rf/dispatch [::frame.events/pan-by (mapv #(/ % ratio) delta-px)]))

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

(defn root
  [orientation options]
  (reagent/with-let [node (react/createRef)
                     last-pointer (atom nil)]
    (let [{:keys [size offset ratio]} options
          horizontal? (= orientation :horizontal)
          stop-drag! #(reset! last-pointer nil)
          margin (/ (- config/track-thickness thumb-thickness) 2)]
      [:div.absolute
       {:ref node
        :class (if horizontal?
                 "bottom-0 left-0"
                 "top-0 right-0")
        :style (if horizontal?
                 {:height config/track-thickness
                  :right config/track-thickness}
                 {:width config/track-thickness
                  :bottom config/track-thickness})
        :on-pointer-down
        (fn [e]
          (.preventDefault e)
          (reset! last-pointer [(.-clientX e) (.-clientY e)])
          (.setPointerCapture (.-current node) (.-pointerId e))
          (when (identical? (.-target e) (.-current node))
            (let [^js rect (.getBoundingClientRect (.-current node))
                  position (if horizontal?
                             (- (.-clientX e) (.-left rect))
                             (- (.-clientY e) (.-top rect)))
                  delta-px (- position (+ offset (/ size 2)))]
              (pan ratio (axis-delta orientation delta-px)))))
        :on-pointer-move
        (fn [e]
          (when-let [[last-x last-y] @last-pointer]
            (let [delta (if horizontal?
                          (- (.-clientX e) last-x)
                          (- (.-clientY e) last-y))]
              (reset! last-pointer [(.-clientX e) (.-clientY e)])
              (when-not (zero? delta)
                (pan ratio (axis-delta orientation delta))))))
        :on-pointer-up stop-drag!
        :on-pointer-cancel stop-drag!}
       [thumb orientation offset size margin]])))
