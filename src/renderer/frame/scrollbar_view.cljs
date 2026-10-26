(ns renderer.frame.scrollbar-view
  "Custom scrollbar implementation for the main canvas."
  (:require
   ["react" :as react]
   [config :as config]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
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

(defn begin-drag
  [node drag orientation {:keys [size offset ratio]}]
  (fn [e]
    (.preventDefault e)
    (.setPointerCapture (.-current node) (.-pointerId e))
    (let [^js rect (.getBoundingClientRect (.-current node))
          horizontal? (= orientation :horizontal)
          position (if horizontal?
                     (- (.-clientX e) (.-left rect))
                     (- (.-clientY e) (.-top rect)))
          track (if horizontal? (.-width rect) (.-height rect))
          clicked-track? (identical? (.-target e) (.-current node))
          target-offset (if clicked-track?
                          (utils.math/clamp (- position (/ size 2))
                                            0
                                            (- track size))
                          offset)]
      (reset! drag {:pointer [(.-clientX e) (.-clientY e)]
                    :ratio ratio
                    :size size
                    :offset target-offset
                    :track track})
      (when clicked-track?
        (pan-by ratio orientation (- target-offset offset))))))

(defn move-drag
  [drag orientation]
  (fn [e]
    (when-let [{:keys [pointer ratio size offset track]} @drag]
      (let [delta (if (= orientation :horizontal)
                    (- (.-clientX e) (pointer 0))
                    (- (.-clientY e) (pointer 1)))
            next-offset (utils.math/clamp (+ offset delta)
                                          0
                                          (- track size))]
        (swap! drag assoc
               :pointer [(.-clientX e) (.-clientY e)]
               :offset next-offset)
        (let [thumb-delta (- next-offset offset)]
          (when-not (zero? thumb-delta)
            (pan-by ratio orientation thumb-delta)))))))

(defn root
  [orientation options]
  (reagent/with-let [node (react/createRef)
                     drag (atom nil)]
    (let [horizontal? (= orientation :horizontal)
          margin (/ (- config/track-thickness thumb-thickness) 2)
          {:keys [size offset]} options]
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
        :on-pointer-down (begin-drag node drag orientation options)
        :on-pointer-move (move-drag drag orientation)
        :on-pointer-up #(reset! drag nil)
        :on-pointer-cancel #(reset! drag nil)}
       [thumb orientation offset size margin]])))
