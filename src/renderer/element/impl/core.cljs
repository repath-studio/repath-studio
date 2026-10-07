(ns renderer.element.impl.core
  (:require
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.element.impl.animation.core]
   [renderer.element.impl.box]
   [renderer.element.impl.container.core]
   [renderer.element.impl.custom.core]
   [renderer.element.impl.descriptive.core]
   [renderer.element.impl.renderable]
   [renderer.element.impl.shape.core]
   [renderer.element.impl.text-content.core]
   [renderer.hierarchy :as hierarchy]))

(hierarchy/derive! ::element.hierarchy/graphics ::element.hierarchy/renderable)
(hierarchy/derive! ::element.hierarchy/gradient ::element.hierarchy/renderable)
(hierarchy/derive! :foreignObject ::element.hierarchy/graphics)
(hierarchy/derive! :linearGradient ::element.hierarchy/gradient)
(hierarchy/derive! :radialGradient ::element.hierarchy/gradient)
