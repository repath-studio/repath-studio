(ns renderer.attribute.impl.clock-value
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#clock-value"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.hierarchy :as hierarchy]
   [renderer.utils.clock :as utils.clock]))

(hierarchy/derive! :dur ::clock-value)
(hierarchy/derive! :begin ::clock-value)
(hierarchy/derive! :end ::clock-value)

(defmethod attribute.hierarchy/initial ::clock-value [_tag _attr] 0)

(defmethod attribute.hierarchy/update-attr [::element.hierarchy/element
                                            ::clock-value]
  ([el k f]
   (update-in el [:attrs k] #(-> utils.clock/->ms % f)))
  ([el k f arg]
   (update-in el [:attrs k] #(-> utils.clock/->ms % (f arg))))
  ([el k f arg & more]
   (update-in el [:attrs k] #(apply f (utils.clock/->ms %) arg more))))
