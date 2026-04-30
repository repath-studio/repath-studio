(ns renderer.attribute.impl.length
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#length"
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.hierarchy :as hierarchy]
   [renderer.i18n.views :as i18n.views]
   [renderer.utils.length :as utils.length]
   [renderer.views :as views]))

(hierarchy/derive! :x ::length)
(hierarchy/derive! :y ::length)
(hierarchy/derive! :x1 ::length)
(hierarchy/derive! :y1 ::length)
(hierarchy/derive! :x2 ::length)
(hierarchy/derive! :y2 ::length)
(hierarchy/derive! :cx ::length)
(hierarchy/derive! :cy ::length)
(hierarchy/derive! :dx ::length)
(hierarchy/derive! :dy ::length)
(hierarchy/derive! ::positive-length ::length)
(hierarchy/derive! :width ::positive-length)
(hierarchy/derive! :height ::positive-length)
(hierarchy/derive! :stroke-width ::positive-length)
(hierarchy/derive! :r ::positive-length)
(hierarchy/derive! :rx ::positive-length)
(hierarchy/derive! :ry ::positive-length)

(defmethod attribute.hierarchy/form-element [::element.hierarchy/element
                                             ::length]
  [_ k v {:keys [disabled placeholder]}]
  [:div.flex.w-full.gap-px
   [attribute.views/form-input k v
    {:class "font-mono"
     :disabled disabled
     :placeholder (if v placeholder "multiple")}]
   [:div.flex.gap-px
    [:button.form-control-button
     {:disabled disabled
      :title (i18n.views/t [::decrease "Decrease"])
      :on-pointer-down #(rf/dispatch [::element.events/update-attr k dec])}
     [views/icon "minus"]]
    [:button.form-control-button
     {:disabled disabled
      :title (i18n.views/t [::increase "Increase"])
      :on-click #(rf/dispatch [::element.events/update-attr k inc])}
     [views/icon "plus"]]]])

(defmethod attribute.hierarchy/initial ::length [_tag _attr] 0)

(defmethod attribute.hierarchy/update-attr ::length
  ([el k f]
   (update-in el [:attrs k] #(utils.length/transform % f)))
  ([el k f arg]
   (update-in el [:attrs k] #(utils.length/transform % f arg)))
  ([el k f arg & more]
   (update-in el [:attrs k] #(apply utils.length/transform % f arg more))))

(defmethod attribute.hierarchy/update-attr ::positive-length
  ([el k f]
   (update-in el [:attrs k] utils.length/transform (fn [v] (max 0 (f v)))))
  ([el k f arg]
   (update-in el [:attrs k] utils.length/transform (fn [v] (max 0 (f v arg)))))
  ([el k f arg & more]
   (update-in el
              [:attrs k]
              utils.length/transform
              (fn [v] (max 0 (apply f v arg more))))))
