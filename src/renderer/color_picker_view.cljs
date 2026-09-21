(ns renderer.color-picker-view
  (:require
   ["@radix-ui/react-select" :as Select]
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
  ["hex" "rgb" "hsl" "lab" "lch" "oklch" "oklab"])

(defn ->css
  [color mode]
  (if (= mode "hex")
    (.hex color)
    (.css color mode)))

(defn set-alpha
  [color mode v]
  (-> (.alpha color v)
      (->css mode)))

(defn selection-input
  [{:keys [color mode on-change on-commit]}]
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
                    (set-alpha mode alpha)
                    (cb)))))]
      [:div.relative.size-full.cursor-crosshair.touch-none.h-40
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
                         (update-position e on-commit))
        :on-pointer-cancel (fn [e]
                             (reset! dragging? false)
                             (update-position e on-commit))}
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

(defn set-hue
  [color mode v]
  (-> (.set color "hsl.h" v)
      (->css mode)))

(defn hue-slider
  [{:keys [color mode on-change on-commit]}]
  [:> Slider/Root
   {:class "relative flex h-4 w-full touch-none px-2"
    :max 359
    :step 1
    :value [(first (.hsl color))]
    :on-value-change (fn [[v]] (on-change (set-hue color mode v)))
    :on-value-commit (fn [[v]] (on-commit (set-hue color mode v)))
    :on-pointer-move #(.stopPropagation %)}
   [:> Slider/Track
    {:class "relative my-0.5 h-3 grow"
     :style {:background (str "linear-gradient(90deg, "
                              (string/join ", " track-colors)
                              ")")}}]
   [:> Slider/Thumb
    {:class "block h-4 w-4 rounded-full bg-primary border border-border"}]])

(defn alpha-slider
  [{:keys [color mode on-change on-commit]}]
  [:> Slider/Root
   {:class "relative flex h-4 w-full touch-none px-2"
    :max 1
    :step 0.01
    :value [(.alpha color)]
    :on-value-change (fn [[v]] (on-change (set-alpha color mode v)))
    :on-value-commit (fn [[v]] (on-commit (set-alpha color mode v)))
    :on-pointer-move #(.stopPropagation %)}
   [:> Slider/Track
    {:class "relative my-0.5 h-3 grow"
     :style {:background (str "repeating-conic-gradient(transparent 0 25%,"
                              "#00000033 0 50%) 50% / 12px 12px")}}
    [:div.absolute.inset-0
     {:style {:background (str "linear-gradient(90deg, transparent, "
                               (.css (.alpha color 1)) ")")}}]
    [:> Slider/Range
     {:class "absolute h-full rounded-full bg-transparent"}]]
   [:> Slider/Thumb
    {:class "block h-4 w-4 rounded-full bg-primary border border-border"}]])

(defn eye-dropper-button
  [{:keys [mode on-commit]}]
  [views/icon-button "eye-dropper"
   {:class "my-1!"
    :on-click #(-> (js/EyeDropper.)
                   (.open)
                   (.then (fn [^js result]
                            (some-> (.-sRGBHex result)
                                    (chroma/Color.)
                                    (->css mode)
                                    (on-commit))))
                   (.catch (fn [_])))}])

(defn mode-select
  [{:keys [color mode on-change]}]
  [:> Select/Root
   {:value mode
    :on-value-change (fn [mode] (on-change (->css color mode)))}
   [:> Select/Trigger
    {:class "button px-2 rounded-sm shrink-0"}
    [:div
     [:> Select/Value ""]
     [:> Select/Icon [views/icon "chevron-down"]]]]
   [:> Select/Portal
    [:> Select/Content
     {:class "menu-content rounded-sm select-content"
      :on-key-down #(.stopPropagation %)
      :on-escape-key-down #(.stopPropagation %)}
     [:> Select/ScrollUpButton
      {:class "select-scroll-button"}
      [views/icon "chevron-up"]]
     (->> supported-types
          (map (fn [format]
                 [:> Select/Item
                  {:value format
                   :class "menu-item px-2!"}
                  [:> Select/ItemText
                   (string/upper-case format)]]))
          (into [:> Select/Viewport {:class "select-viewport"}]))
     [:> Select/ScrollDownButton
      {:class "select-scroll-button"}
      [views/icon "chevron-down"]]]]])

(defn valid-channel-value?
  [channel value]
  (if (= channel "hex")
    (chroma/valid value)
    (and (seq (string/trim value))
         (not (js/isNaN value)))))

(defn set-value
  [e {:keys [value color mode channel on-commit]}]
  (let [raw (.. e -target -value)
        new-value (cond-> raw
                    (not= channel "hex")
                    (js/parseFloat raw))]
    (if-not (valid-channel-value? channel raw)
      (set! (.. e -target -value) value)
      (-> (case channel
            "alpha" (.alpha color new-value)
            "hex" (chroma/Color. new-value)
            (.set color (str mode "." channel) new-value))
          (->css mode)
          (on-commit)))))

(defn index->channel
  [index mode]
  (or (if (= mode "hex")
        "hex"
        (get (vec (take-last 3 mode)) index))
      "alpha"))

(defn channel-input
  [{:keys [value index mode]
    :as options}]
  (let [channel (index->channel index mode)
        value (cond-> value
                (number? value)
                (utils.attribute/->fixed 2))
        options (merge options {:value value
                                :channel channel})]
    [:div.flex.flex-col.items-center.w-full.text-2xs
     [:input.form-element.text-center.p-0!
      {:id channel
       :default-value value
       :on-blur #(set-value % options)
       :on-key-down #(utils.key/down-handler % value set-value options)}]
     [:label.text-foreground-muted.uppercase
      {:for channel}
      channel]]))

(defn channel-values
  [{:keys [color mode]}]
  (case mode
    "hex" [(.hex color)]
    "rgb" (.rgba color)
    "hsl" (.hsl color)
    "lab" (.lab color)
    "lch" (.lch color)
    "oklch" (.oklch color)
    "oklab" (.oklab color)))

(defn channels
  [options]
  (->> (channel-values options)
       (map-indexed (fn [index value]
                      ^{:key (str index value)}
                      [channel-input (merge options {:value value
                                                     :index index})]))
       (into [:div.flex.w-full.items-center.rounded-sm.gap-1])))

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
  [{:keys [value on-change on-commit dropper]}]
  (let [color (if (chroma/valid value)
                (chroma/Color. value)
                (chroma/Color. "black"))
        value (string/lower-case (str value))
        mode (or (some #(when (string/starts-with? value %) %) supported-types)
                 "hex")
        options {:color color
                 :mode mode
                 :on-change on-change
                 :on-commit on-commit}]
    [:div.flex.flex-col.gap-4.w-70.p-2
     {:dir "ltr"}
     [:div.flex.flex-col.gap-4
      [selection-input options]
      [:div.flex.items-center.gap-1.justify-center
       (when dropper
         [eye-dropper-button options])
       [:div.flex.flex-col.flex-1.space-between.gap-1
        [hue-slider options]
        [alpha-slider options]]]
      [:div.flex.items-center.gap-2
       [mode-select options]
       [channels options]]]
     [mdn-button mode]]))
