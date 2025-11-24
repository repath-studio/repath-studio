(ns renderer.element.impl.container.svg
  "https://www.w3.org/TR/SVG/struct.html#SVGElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/svg"
  (:require
   [re-frame.core :as rf]
   [renderer.a11y.subs :as-alias a11y.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.event.impl.pointer :as event.impl.pointer]))

(derive :svg ::element.hierarchy/container)

(defmethod element.hierarchy/properties :svg
  []
  {:icon "svg"
   :description [::description
                 "The svg element is a container that defines a new
                  coordinate system and viewport. It is used as the outermost
                  element of SVG documents, but it can also be used to embed
                  an SVG fragment inside an SVG or HTML document."]
   :attrs [:overflow]})

(defmethod element.hierarchy/render :svg
  [el]
  (let [attrs (:attrs el)
        child-els @(rf/subscribe [::element.subs/filter-visible (:children el)])
        rect-attrs (select-keys attrs [:x :y :width :height])
        text-attrs (select-keys attrs [:x :y])
        active-filter @(rf/subscribe [::a11y.subs/active-filter])
        zoom @(rf/subscribe [::document.subs/zoom])
        pointer-handler (partial event.impl.pointer/handler! el)
        shadow-size (/ 2 zoom)]
    [:g
     [:text
      (merge
       (update text-attrs :y - (/ 10 zoom))
       {:on-pointer-up pointer-handler
        :on-pointer-down pointer-handler
        :on-pointer-move pointer-handler
        :fill "gray"
        :font-size (/ 12 zoom)}) (or (:label el) (name (:tag el)))]

     [:rect
      (merge
       rect-attrs
       {:fill "rgba(0, 0, 0, .1)"
        :transform (str "translate(" shadow-size " " shadow-size ")")
        :style {:filter (str "blur(" shadow-size "px)")}})]

     [:svg
      (cond-> attrs
        :always
        (dissoc :style)

        active-filter
        (assoc :filter (str "url(#" (name active-filter) ")")))
      [:rect
       (merge
        rect-attrs
        {:x 0
         :y 0
         :fill "white"
         :on-pointer-up pointer-handler
         :on-pointer-down #(when (= (.-button %) 2)
                             (pointer-handler %))})]
      (for [el child-els]
        ^{:key (:id el)}
        [element.hierarchy/render el])]]))
