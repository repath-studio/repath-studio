(ns renderer.attribute.impl.length
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#length"
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.i18n.views :as i18n.views]
   [renderer.utils.length :as utils.length]
   [renderer.views :as views]))

(attribute.hierarchy/derive-attribute :x ::length)
(attribute.hierarchy/derive-attribute :y ::length)
(attribute.hierarchy/derive-attribute :x1 ::length)
(attribute.hierarchy/derive-attribute :y1 ::length)
(attribute.hierarchy/derive-attribute :x2 ::length)
(attribute.hierarchy/derive-attribute :y2 ::length)
(attribute.hierarchy/derive-attribute :cx ::length)
(attribute.hierarchy/derive-attribute :cy ::length)
(attribute.hierarchy/derive-attribute :dx ::length)
(attribute.hierarchy/derive-attribute :dy ::length)
(attribute.hierarchy/derive-attribute ::positive-length ::length)
(attribute.hierarchy/derive-attribute :width ::positive-length)
(attribute.hierarchy/derive-attribute :height ::positive-length)
(attribute.hierarchy/derive-attribute :stroke-width ::positive-length)
(attribute.hierarchy/derive-attribute :r ::positive-length)
(attribute.hierarchy/derive-attribute :rx ::positive-length)
(attribute.hierarchy/derive-attribute :ry ::positive-length)

(defmethod attribute.hierarchy/form-element [:default ::length]
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
