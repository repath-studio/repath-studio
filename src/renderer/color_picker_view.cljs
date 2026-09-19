(ns renderer.color-picker-view
  (:require
   ["@radix-ui/react-select" :as Select]
   ["@radix-ui/react-slider" :as Slider]
   ["chroma-js" :as chroma]
   [clojure.string :as string]
   [reagent.core :as reagent]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.math :as utils.math]
   [renderer.views :as views]))

(def supported-types
  ["hex" "rgb" "hsl" "lab" "oklab" "lch" "oklch"])

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
  (reagent/with-let [container (atom nil)
                     dragging? (atom false)]
    (let [[hue saturation lightness] (.hsl color)
          hue (if (js/isNaN hue) 0 hue)
          alpha (.alpha color)
          position [saturation (utils.math/clamp lightness 0 1)]
          set-node (fn [node] (reset! container node))
          update-position
          (fn [event cb]
            (when-let [^js rect (.getBoundingClientRect @container)]
              (let [x (utils.math/clamp (/ (- (.-clientX event) (.-left rect))
                                           (.-width rect))
                                        0 1)
                    y (utils.math/clamp (/ (- (.-clientY event) (.-top rect))
                                           (.-height rect))
                                        0 1)]
                (-> (chroma/hsl hue x (- 1 y))
                    (.alpha alpha)
                    (->css mode)
                    (cb)))))]
      [:div.relative.size-full.cursor-crosshair.touch-none.rounded.h-50
       {:ref set-node
        :style {:background
                (str "linear-gradient(0deg, rgba(0,0,0,1), rgba(0,0,0,0)), "
                     "linear-gradient(90deg, rgba(255,255,255,1), "
                     "rgba(2, 2, 2, 0)), "
                     (-> (chroma/hsl hue 1 0.5) .css))}
        :on-pointer-down (fn [e]
                           (.preventDefault e)
                           (some-> @container
                                   (.setPointerCapture (.-pointerId e)))
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
                 :top (str (* 100 (- 1 (second position))) "%")
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
  [:> Select/Root
   {:value mode
    :on-value-change (fn [mode] (on-value-change (->css color mode)))}
   [:> Select/Trigger
    {:class "button px-2 rounded-sm shrink-0"}
    [:div.flex.gap-1.items-center
     [:> Select/Value (string/upper-case mode)]
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

(defn format-input
  [v]
  [:input.form-element.bg-secondary!.p-2!.h-full!
   {:dir "ltr"
    :disabled true
    :value (cond-> v
             (number? v)
             (-> (js/parseFloat)
                 (utils.attribute/->fixed)))}])

(defn color-values
  [^js color mode]
  (case mode
    "hex" [(.hex color)]
    "rgb" (.rgba color)
    "hsl" (.hsl color)
    "lab" (.lab color)
    "lch" (.lch color)
    "oklab" (.oklab color)
    "oklch" (.oklch color)))

(defn root
  [{:keys [value on-change on-change-complete dropper]}]
  (let [color (if (chroma/valid value)
                (chroma/Color. value)
                (chroma/Color. "black"))
        value (string/lower-case (str value))
        mode (or (some #(when (string/starts-with? value %) %) supported-types)
                 "hex")]
    [:div.flex.flex-col.gap-4.w-70
     [selection-input color mode on-change on-change-complete]
     [hue-slider color mode on-change on-change-complete]
     [alpha-slider color mode on-change on-change-complete]
     [:div.flex.items-center.gap-2
      (when dropper
        [eye-dropper-button mode on-change-complete])
      [mode-select color mode on-change-complete]
      (->> (color-values color mode)
           (map format-input)
           (into [:div.flex.w-full.items-center.rounded-sm.gap-px]))]]))
