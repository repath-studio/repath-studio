(ns renderer.element.impl.custom.core
  (:require
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.impl.custom.blob]
   [renderer.element.impl.custom.brush]
   [renderer.element.impl.custom.canvas]
   [renderer.element.impl.custom.guide]
   [renderer.hierarchy :as hierarchy]))

(hierarchy/derive! ::element.hierarchy/custom ::element.hierarchy/renderable)
