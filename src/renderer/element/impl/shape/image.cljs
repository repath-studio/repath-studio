(ns renderer.element.impl.shape.image
  "https://www.w3.org/TR/SVG/embedded.html#ImageElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/image"
  (:require
   [re-frame.core :as rf]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.hierarchy :as hierarchy]
   [renderer.tool.subs :as-alias tool.subs]))

(hierarchy/derive! :image ::element.hierarchy/graphics)
(hierarchy/derive! :image ::element.hierarchy/box)

(defmethod element.hierarchy/properties :image
  []
  {:icon "image"
   :label [::label "Image"]
   :description [::description
                 "The <image> SVG element includes images inside SVG documents.
                  It can display raster image files or other SVG files."]
   :attrs [:href]})

(defmethod element.hierarchy/render :image
  [el]
  (let [child-els @(rf/subscribe [::element.subs/filter-visible (:children el)])
        idle? @(rf/subscribe [::tool.subs/idle?])]
    [element.views/render-to-dom el child-els idle?]))
