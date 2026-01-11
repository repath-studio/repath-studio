(ns renderer.utils.attribute
  (:require
   ["@mdn/browser-compat-data" :as bcd]
   ["mdn-data" :as mdn]
   [camel-snake-kebab.core :as camel-snake-kebab]
   [clojure.string :as string]
   [malli.core :as m]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.db :as element.db :refer [ElementAttrs ElementTag]]
   [renderer.element.hierarchy :as element.hierarchy]))

(def mdn-data
  "https://github.com/mdn/data/blob/main/docs/updating_css_json.md"
  (js->clj mdn :keywordize-keys true))

(def svg-data
  "https://github.com/mdn/browser-compat-data"
  (js->clj (.-svg bcd) :keywordize-keys true))

(def core #{:id :class :style})

(def presentation
  #{:text-anchor :text-rendering :font-style :mask :image-rendering
    :stroke-dasharray :fill-rule :font-stretch :text-overflow :vector-effect
    :stroke :stop-color :clip :glyph-orientation-horizontal :solid-opacity
    :transform :color :white-space :font-size :kerning :font-variant
    :writing-mode :font-weight :overflow :clip-rule :stroke-opacity :fill
    :color-profile :stroke-linejoin :shape-rendering :cursor :stroke-dashoffset
    :word-spacing :clip-path :stroke-linecap :flood-opacity :lighting-color
    :alignment-baseline :dominant-baseline :marker-start :filter :stroke-width
    :opacity :baseline-shift :color-interpolation-filters :transform-origin
    :text-decoration :display :stroke-miterlimit :letter-spacing :flood-color
    :unicode-bidi :marker-mid :pointer-events :font-size-adjust
    :glyph-orientation-vertical :color-interpolation :visibility
    :enable-background :direction :fill-opacity :solid-color :font-family
    :marker-end :paint-order})

(def order
  [:href
   :d
   :points
   :x :y
   :x1 :y1
   :x2 :y2
   :cx :cy
   :dx :dy
   :width :height
   :rx :ry
   :r
   :rotate
   :transform
   :font-family :font-size :font-weight :font-style
   :textLength
   :lengthAdjust
   :viewBox :preserveAspectRatio
   :stroke
   :fill
   :stroke-width :stroke-linecap :stroke-linejoin :stroke-dasharray
   :crossorigin
   :decoding
   :opacity
   :overflow
   :id :class :tab-index
   :style])

(def weight-name-mapping
  "https://developer.mozilla.org/en-US/docs/Web/CSS/@font-face/font-weight#common_weight_name_mapping"
  {"100" ["Thin" "Hairline"]
   "200" ["ExtraLight" "UltraLight"]
   "300" ["Light"]
   "400" ["Normal" "Regular" "Book"]
   "500" ["Medium"]
   "600" ["SemiBold" "DemiBold"]
   "700" ["Bold"]
   "800" ["ExtraBold" "UltraBold"]
   "900" ["Black" "Heavy"]})

(def camelcased
  ["accentHeight"
   "alignmentBaseline"
   "allowReorder"
   "arabicForm"
   "attributeName"
   "attributeType"
   "autoReverse"
   "baseFrequency"
   "baseProfile"
   "baselineShift"
   "calcMode"
   "capHeight"
   "clipPath"
   "clipPathUnits"
   "clipRule"
   "colorInterpolation"
   "colorInterpolationFilters"
   "colorProfile"
   "colorRendering"
   "contentScriptType"
   "contentStyleType"
   "diffuseConstant"
   "dominantBaseline"
   "edgeMode"
   "enableBackground"
   "externalResourcesRequired"
   "fillOpacity"
   "fillRule"
   "filterRes"
   "filterUnits"
   "floodColor"
   "floodOpacity"
   "fontFamily"
   "fontSize"
   "fontSizeAdjust"
   "fontStretch"
   "fontStyle"
   "fontVariant"
   "glyphName"
   "glyphOrientationHorizontal"
   "glyphOrientationVertical"
   "glyphRef"
   "gradientTransform"
   "gradientUnits"
   "horizAdvX"
   "horizOriginX"
   "imageRendering"
   "kernelMatrix"
   "kernelUnitLength"
   "keyPoints"
   "keySplines"
   "keyTimes"
   "lengthAdjust"
   "letterSpacing"
   "lightingColor"
   "limitingConeAngle"
   "markerEnd"
   "markerHeight"
   "markerMid"
   "markerStart"
   "markerUnits"
   "markerWidth"
   "maskContentUnits"
   "maskUnits"
   "mathematical"
   "numOctaves"
   "overlinePosition"
   "overlineThickness"
   "paintOrder"
   "pathLength"
   "patternContentUnits"
   "patternTransform"
   "patternUnits"
   "pointerEvents"
   "pointsAtX"
   "pointsAtY"
   "pointsAtZ"
   "preserveAlpha"
   "preserveAspectRatio"
   "primitiveUnits"
   "refX"
   "refY"
   "renderingIntent"
   "repeatCount"
   "repeatDur"
   "requiredExtensions"
   "requiredFeatures"
   "shapeRendering"
   "specularConstant"
   "specularExponent"
   "spreadMethod"
   "startOffset"
   "stdDeviation"
   "stitchTiles"
   "stopColor"
   "stopOpacity"
   "strikethroughPosition"
   "strikethroughThickness"
   "strokeDasharray"
   "strokeDashoffset"
   "strokeLinecap"
   "strokeLinejoin"
   "strokeMiterlimit"
   "strokeOpacity"
   "strokeWidth"
   "surfaceScale"
   "systemLanguage"
   "tableValues"
   "targetX"
   "targetY"
   "textAnchor"
   "textDecoration"
   "textLength"
   "textRendering"
   "underlinePosition"
   "underlineThickness"
   "unicodeBidi"
   "unicodeRange"
   "unitsPerEm"
   "vAlphabetic"
   "vHanging"
   "vIdeographic"
   "vMathematical"
   "vectorEffect"
   "vertAdvY"
   "vertOriginX"
   "vertOriginY"
   "viewBox"
   "viewTarget"
   "wordSpacing"
   "writingMode"
   "xChannelSelector"
   "xHeight"
   "xlinkActuate"
   "xlinkArcrole"
   "xlinkHref"
   "xlinkRole"
   "xlinkShow"
   "xlinkTitle"
   "xlinkType"
   "xmlnsXlink"
   "xmlBase"
   "xmlLang"
   "xmlSpace"
   "yChannelSelector"
   "zoomAndPan"])

(def lowercased (mapv string/lower-case camelcased))

(m/=> compatibility [:function
                     [:-> ElementTag [:maybe map?]]
                     [:-> ElementTag keyword? [:maybe map?]]])
(defn compatibility
  "Returns compatibility data for tags or attributes."
  ([tag]
   (-> svg-data :elements tag :__compat))
  ([tag attr]
   (or (-> svg-data :elements tag attr :__compat)
       (-> svg-data :global_attributes attr :__compat))))

(defn enhance-data-readability
  [property k]
  (cond-> property
    (and (get property k)
         (string? (get property k)))
    (update k #(-> (camel-snake-kebab/->kebab-case-string %)
                   (string/replace "-" " ")))))

(m/=> property-data [:-> keyword? any?])
(defn property-data
  [k]
  (let [css-property (get-in mdn-data [:css :properties k])]
    (reduce enhance-data-readability
            css-property
            [:appliesto :computed :percentages :animationType])))

(def property-data-memo (memoize property-data))

(m/=> mdn-url [:-> keyword? [:maybe map?] [:maybe string?]])
(defn mdn-url
  [k data]
  (or (:mdn_url data)
      (when data
        (str "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/"
             (name k)))
      (:mdn_url (property-data-memo k))))

(m/=> spec-url [:-> keyword? [:maybe map?] [:maybe string?]])
(defn spec-url
  [k data]
  (let [url (or (:spec_url data)
                (:href (property-data-memo k)))]
    (cond-> url
      (vector? url)
      (first))))

(def whitespace-regex #"\s*[\s,]\s*")

(m/=> str->seq [:-> string? vector?])
(defn str->seq
  [s]
  (-> s string/trim (string/split whitespace-regex)))

(m/=> points->vec [:function
                   [:-> string? vector?]
                   [:-> string? pos-int? vector?]])
(defn points->vec
  ([points]
   (points->vec points 2))
  ([points step]
   (into [] (partition-all step) (str->seq points))))

(m/=> ->camel-case [:-> keyword? keyword?])
(defn ->camel-case
  [k]
  (let [index (->> k name string/lower-case (.indexOf lowercased))]
    (-> (if (= index -1) k (get camelcased index))
        (camel-snake-kebab/->camelCaseString)
        (keyword))))

(def ->camel-case-memo (memoize ->camel-case))

(m/=> ->attrs [:-> map? ElementAttrs])
(defn ->attrs
  [attrs]
  (let [deprecated-path [:__compat :status :deprecated]
        filtered-attrs (->> attrs
                            (filter #(not (get-in (val %) deprecated-path)))
                            (into {}))]
    (-> filtered-attrs
        (dissoc :__compat :systemLanguage :fetchpriority)
        (keys)
        (zipmap (repeat "")))))

(def ->attrs-memo (memoize ->attrs))

(m/=> defaults [:-> ElementTag ElementAttrs])
(defn defaults
  [tag]
  (merge (when (element.db/tag? tag)
           (merge (->attrs-memo (or (tag (:elements svg-data)) {}))
                  (when (or (isa? @element.hierarchy/hierarchy
                                  tag
                                  ::element.hierarchy/shape)
                            (isa? @element.hierarchy/hierarchy
                                  tag
                                  ::element.hierarchy/container))
                    (zipmap core (repeat "")))))
         (when (contains? #{:animateMotion :animateTransform} tag)
           (->attrs-memo (:animate (:elements svg-data))))
         (zipmap (:attrs (element.hierarchy/properties tag)) (repeat ""))))

(def defaults-memo (memoize defaults))

(m/=> initial [:-> ElementTag keyword? string?])
(defn initial
  [tag k]
  (let [dispatchable? (contains? (methods attribute.hierarchy/initial) [tag k])
        dispatch-tag (if dispatchable? tag :default)]
    (or (attribute.hierarchy/initial dispatch-tag k)
        (let [initial-value (:initial (property-data-memo k))]
          (when (string? initial-value) initial-value)))))

(def initial-memo (memoize initial))

(m/=> defaults-with-vals [:-> ElementTag ElementAttrs])
(defn defaults-with-vals
  [tag]
  (->> (defaults-memo tag)
       (map (fn [[k v]] [k (or (initial-memo tag k) v)]))
       (into {})))
