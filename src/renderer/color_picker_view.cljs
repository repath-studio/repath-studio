(ns renderer.color-picker-view
  (:require
   ["@radix-ui/react-slider" :as Slider]
   ["chroma-js" :as chroma]
   ["react" :as react]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.events :as events]
   [renderer.i18n.views :as i18n.views]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.key :as utils.key]
   [renderer.utils.math :as utils.math]
   [renderer.views :as views]))

(def supported-types
  ["hex" "rgb" "hsl" "lab" "lch"])

(defn ->css
  [^js color mode]
  (if (= mode "hex")
    (.hex color)
    (.css color mode)))

(def alpha-checkerboard
  (str "data:image/png;base64,"
       "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA"
       "MUlEQVQ4T2NkYGAQYcAP3uCTZhw1gGGYhAGBZIA/nYDCgBDA"
       "m9BGDWAAJyRCgLaBCAAgXwixzAS0pgAAAABJRU5ErkJggg=="))

(defn selection-input
  [^js color mode on-change on-complete]
  (reagent/with-let [dragging? (atom false)]
    (let [[hue saturation lightness] (.hsl color)
          hue (if (js/isNaN hue) 0 hue)
          alpha (.alpha color)
          top-lightness (fn [x] (+ 0.5 (* 0.5 (- 1 x))))
          position [saturation lightness]
          node (react/createRef)
          update-position
          (fn [event cb]
            (when-let [^js rect (.getBoundingClientRect (.-current node))]
              (let [x (utils.math/clamp (/ (- (.-clientX event) (.-left rect))
                                           (.-width rect))
                                        0 1)
                    y (utils.math/clamp (/ (- (.-clientY event) (.-top rect))
                                           (.-height rect))
                                        0 1)]
                (-> (chroma/hsl hue x (* (top-lightness x) (- 1 y)))
                    (.alpha alpha)
                    (->css mode)
                    (cb)))))]
      [:div.relative.size-full.cursor-crosshair.touch-none.rounded.h-50
       {:ref node
        :style {:background
                (str "linear-gradient(0deg, rgba(0,0,0,1), rgba(0,0,0,0)), "
                     "linear-gradient(90deg, rgba(255,255,255,1), "
                     "rgba(2, 2, 2, 0)), "
                     (-> (chroma/hsl hue 1 0.5) .css))}
        :on-pointer-down (fn [e]
                           (.preventDefault e)
                           (.setPointerCapture (.-current node) (.-pointerId e))
                           (reset! dragging? true)
                           (update-position e on-change))
        :on-pointer-move (fn [e]
                           (when @dragging?
                             (update-position e on-change)))
        :on-pointer-up (fn [e]
                         (reset! dragging? false)
                         (update-position e on-complete))
        :on-pointer-cancel (fn [e]
                             (reset! dragging? false)
                             (update-position e on-complete))}
       [:div.absolute.h-4.w-4.rounded-full.border-2.border-white
        {:class "-translate-x-1/2 -translate-y-1/2 pointer-events-none"
         :style {:left (str (* 100 (first position)) "%")
                 :top (str (* 100 (- 1 (/ lightness
                                          (top-lightness saturation)))) "%")
                 :box-shadow "0 0 0 1px rgba(0,0,0,0.5)"}}]])))

(def track-colors
  ["#ff0000"
   "#ffff00"
   "#00ff00"
   "#00ffff"
   "#0000ff"
   "#ff00ff"
   "#ff0000"])

(defn hue-slider
  [^js color mode on-change on-commit]
  [:> Slider/Root
   {:class "relative flex h-4 w-full touch-none"
    :max 359
    :step 1
    :value [(first (.hsl color))]
    :on-value-change (fn [[v]] (-> (.set color "hsl.h" v)
                                   (->css mode)
                                   (on-change)))
    :on-value-commit (fn [[v]] (-> (.set color "hsl.h" v)
                                   (->css mode)
                                   (on-commit)))
    :on-pointer-move #(.stopPropagation %)}
   [:> Slider/Track
    {:class "relative my-0.5 h-3 grow rounded-full"
     :style {:background (str "linear-gradient(90deg, "
                              (string/join ", " track-colors)
                              ")")}}]
   [:> Slider/Thumb
    {:class "block h-4 w-4 rounded-full bg-foreground-hovered shadow-sm"}]])

(defn alpha-slider
  [^js color mode on-change on-commit]
  [:> Slider/Root
   {:class "relative flex h-4 w-full touch-none"
    :max 1
    :step 0.01
    :value [(.alpha color)]
    :on-value-change (fn [[v]] (-> (.alpha color v) (->css mode) (on-change)))
    :on-value-commit (fn [[v]] (-> (.alpha color v) (->css mode) (on-commit)))
    :on-pointer-move #(.stopPropagation %)}
   [:> Slider/Track
    {:class "relative my-0.5 h-3 grow rounded-full"
     :style {:background (str "url(\"" alpha-checkerboard "\") left center")}}
    [:div.absolute.inset-0.rounded-full
     {:style {:background (str "linear-gradient(90deg, transparent, "
                               (.css color)
                               ")")}}]
    [:> Slider/Range
     {:class "absolute h-full rounded-full bg-transparent"}]]
   [:> Slider/Thumb
    {:class "block h-4 w-4 rounded-full bg-foreground-hovered shadow-sm"}]])

(defn eye-dropper-button
  [mode on-pick]
  [views/icon-button "eye-dropper"
   {:on-click #(-> (js/EyeDropper.)
                   (.open)
                   (.then (fn [^js result]
                            (some-> (.-sRGBHex result)
                                    (chroma/Color.)
                                    (->css mode)
                                    (on-pick))))
                   (.catch (fn [_])))}])

(defn mode-select
  [color mode on-value-change]
  [views/icon-button "chevron-down"
   {:on-click #(let [i (.indexOf supported-types mode)
                     next-i (if (= i (dec (count supported-types))) 0 (inc i))]
                 (->> (get supported-types next-i)
                      (->css color)
                      (on-value-change)))}])

(defn set-value
  [e v color mode channel on-commit]
  (let [new-v (-> (.. e -target -value) js/parseFloat)]
    (js/console.log channel)
    (if (js/isNaN new-v)
      (set! (.. e -target -value) v)
      (-> (if channel
            (.set color (str mode "." channel) v)
            (.alpha color v))
          (->css mode)
          (on-commit)))))

(defn channel-input
  [v index color mode on-commit]
  (let [channel (if (= mode "hex")
                  "hex"
                  (get mode index))
        value (cond-> v
                (number? v)
                (-> (js/parseFloat)
                    (utils.attribute/->fixed)))]
    [:div.flex.flex-col.items-center.w-full
     [:input.form-element.text-center
      {:dir "ltr"
       :id channel
       :default-value value
       :on-blur #(set-value % value color mode channel on-commit)
       :on-key-down #(utils.key/down-handler % value
                                             set-value
                                             value color mode channel
                                             on-commit)}]
     [:label
      {:for channel}
      (string/upper-case (or channel "a"))]]))

(defn channel-values
  [^js color mode]
  (case mode
    "hex" [(.hex color)]
    "rgb" (.rgba color)
    "hsl" (.hsl color)
    "lab" (.lab color)
    "lch" (.lch color)))

(defn color-url
  [mode]
  (if (= mode "hex")
    "https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Values/hex-color"
    (str "https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Values/color_value/"
         mode)))

(defn mdn-button
  [mode]
  (let [url (color-url mode)]
    [:button.button.px-3.flex-1.rounded
     {:on-click #(rf/dispatch [::events/open-remote-url url])}
     (i18n.views/t [:learn-more ["Learn more about %1"]]
                   [(string/upper-case mode)])]))

(defn root
  [{:keys [value on-change on-change-complete dropper]}]
  (let [color (if (chroma/valid value)
                (chroma/Color. value)
                (chroma/Color. "black"))
        value (string/lower-case (str value))
        mode (or (some #(when (string/starts-with? value %) %) supported-types)
                 "hex")]
    [:dev.flex.flex-col.gap-4.w-70
     [:div.flex.flex-col.gap-4
      [selection-input color mode on-change on-change-complete]
      [hue-slider color mode on-change on-change-complete]
      [alpha-slider color mode on-change on-change-complete]
      [:div.flex.items-center.gap-2
       (when dropper
         [eye-dropper-button mode on-change-complete])
       [mode-select color mode on-change-complete]
       (->> (channel-values color mode)
            (map-indexed (fn [i v]
                           ^{:key (str mode i v)}
                           [channel-input v i color mode on-change-complete]))
            (into [:div.flex.w-full.items-center.rounded-sm.gap-px]))]]
     [mdn-button mode]]))
