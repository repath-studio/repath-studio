(ns renderer.utils.font
  (:require
   ["opentype.js" :as opentype]
   [clojure.string :as string]
   [malli.core :as m]
   [renderer.app.db :refer [SystemFonts]]
   [renderer.db :refer [JS_Array JS_Object JS_Promise]]
   [renderer.element.db :refer [Element]]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.dom :as utils.dom]
   [renderer.utils.element :as utils.element]))

(m/=> get-computed-styles! [:-> Element [:maybe map?]])
(defn get-computed-styles!
  [{:keys [content]
    :as el}]
  (when-let [svg (utils.dom/canvas-element!)]
    (let [dom-el (utils.element/->dom-element el)]
      (.appendChild svg dom-el)
      (set! (.-innerHTML dom-el) (if (empty? content) "\u00a0" content))
      (let [computed-style (.getComputedStyle js/window dom-el nil)
            font-style (.getPropertyValue computed-style "font-style")
            font-size (.getPropertyValue computed-style "font-size")
            font-weight (.getPropertyValue computed-style "font-weight")
            bbox (utils.bounds/dom-el->bbox dom-el)]
        (.remove dom-el)
        {:font-style font-style
         :font-size font-size
         :font-weight font-weight
         :bbox bbox}))))

(m/=> font-data->path-data! [:-> JS_Object string? map? JS_Promise])
(defn font-data->path-data!
  [^js/FontData font-data text props]
  (let [{:keys [x y font-size]} props]
    (-> (.blob font-data)
        (.then (fn [^js/Blob blob]
                 (-> (.arrayBuffer blob)
                     (.then (fn [buffer]
                              (let [font (opentype/parse buffer)
                                    path (.getPath font text x y font-size)]
                                (.toPathData path))))))))))

(m/=> includes-prop? [:-> string? string? boolean?])
(defn includes-prop?
  [v prop]
  (when v
    (string/includes? (string/lower-case v)
                      (string/lower-case prop))))

(m/=> match-font-by-weight [:->
                            string?
                            [:sequential JS_Object]
                            [:sequential JS_Object]])
(defn match-font-by-weight
  [weight fonts]
  (let [weight-num (js/parseInt weight)
        weight-names (get utils.attribute/weight-name-mapping weight)
        includes-weight? (fn [font]
                           (->> weight-names
                                (some #(includes-prop? % (.-style font)))))
        weights (filter includes-weight? fonts)]
    (if (or (seq weights) (< weight-num 100))
      weights
      (recur (str (- weight-num 100)) fonts))))

(m/=> match-font [:-> JS_Array string? string? string? [:maybe JS_Object]])
(defn match-font
  [fonts family style weight]
  (let [families (filter #(includes-prop? family (.-family %)) fonts)
        styles (filter #(includes-prop? style (.-style %)) families)
        weights (match-font-by-weight weight (if (seq styles) styles families))]
    (or (first weights)
        (first styles)
        (first families)
        (first fonts))))

(m/=> default-font-path [:-> string? string? string?])
(defn default-font-path
  [font-style font-weight]
  (str "./css/files/noto-sans-latin-" font-weight "-" font-style ".woff"))

(m/=> font-data->system-fonts [:-> JS_Array SystemFonts])
(defn font-data->system-fonts
  [available-fonts]
  (->> available-fonts
       (reduce (fn [fonts ^js/FontData font-data]
                 (let [family (.-family font-data)
                       style (.-style font-data)]
                   (assoc-in fonts [family style]
                             {:postscript-name (.-postscriptName font-data)
                              :full-name (.-fullName font-data)}))) {})))
