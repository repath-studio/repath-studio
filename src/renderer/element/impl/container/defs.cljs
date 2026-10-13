(ns renderer.element.impl.container.defs
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/defs"
  (:require
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.impl.container.group]
   [renderer.element.impl.container.svg]
   [renderer.hierarchy :as hierarchy]))

(hierarchy/derive! :defs ::element.hierarchy/container)

(defmethod element.hierarchy/bbox :defs [_el _transform])
