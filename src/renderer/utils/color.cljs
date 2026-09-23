(ns renderer.utils.color
  (:require
   ["chroma-js" :as chroma]
   [clojure.string :as string]
   [malli.core :as m]
   [renderer.db :refer [JS_Object JS_Array ColorChannel ColorNotation]]))

(def supported-notations (rest ColorNotation))

(m/=> string->notation [:-> string? ColorNotation])
(defn string->notation
  [s]
  (or (some #(when (-> (string/lower-case s)
                       (string/starts-with? %)) %)
            supported-notations)
      "hex"))

(m/=> string->color [:function
                     [:-> string? JS_Object]
                     [:-> string? string? JS_Object]])
(defn string->color
  ([s]
   (string->color s "black"))
  ([s default]
   (chroma/Color. (if (chroma/valid s) s default))))

(m/=> hsl->color [:-> number? number? number? JS_Object])
(defn hsl->color
  [hue saturation lightness]
  (chroma/hsl hue saturation lightness))

(m/=> ->css [:function
             [:-> JS_Object string?]
             [:-> JS_Object ColorNotation string?]])
(defn ->css
  ([^js color]
   (->css color "hex"))
  ([^js color notation]
   (if (= notation "hex")
     (.hex color)
     (.css color notation))))

(m/=> set-alpha [:-> JS_Object ColorNotation number? string?])
(defn set-alpha
  [^js color notation v]
  (-> (.alpha color v)
      (->css notation)))

(m/=> set-hue [:-> JS_Object ColorNotation number? string?])
(defn set-hue
  [^js color notation v]
  (-> (.set color "hsl.h" v)
      (->css notation)))

(m/=> valid-channel-value? [:-> ColorChannel string? boolean?])
(defn valid-channel-value?
  [channel v]
  (boolean (if (= channel "hex")
             (chroma/valid v)
             (and (seq (string/trim v))
                  (not (js/isNaN v))))))

(m/=> index->channel [:-> int? ColorNotation ColorChannel])
(defn index->channel
  [index notation]
  (or (if (= notation "hex")
        "hex"
        (-> (take-last 3 notation)
            (vec)
            (get index)))
      "alpha"))

(m/=> channel-values [:-> JS_Object ColorNotation JS_Array])
(defn channel-values
  [^js color notation]
  (case notation
    "hex" #js [(.hex color)]
    "rgb" (.rgba color)
    "hsl" (.hsl color)
    "lab" (.lab color)
    "lch" (.lch color)
    "oklch" (.oklch color)
    "oklab" (.oklab color)))
