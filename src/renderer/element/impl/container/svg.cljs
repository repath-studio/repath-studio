(ns renderer.element.impl.container.svg
  "https://www.w3.org/TR/SVG/struct.html#SVGElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/svg"
  (:require
   ["react" :as react]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.a11y.subs :as-alias a11y.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.hierarchy :as hierarchy]
   [renderer.i18n.views :as i18n.views]
   [renderer.input.impl.pointer :as input.impl.pointer]
   [renderer.utils.element :as utils.element]
   [renderer.utils.transform :as utils.transform]))

(hierarchy/derive! :svg ::element.hierarchy/container)
(hierarchy/derive! :svg ::element.hierarchy/box)

(def label [::label "SVG"])

(defmethod element.hierarchy/properties :svg
  []
  {:icon "svg"
   :top-level true
   :label label
   :description [::description
                 "The svg element is a container that defines a new
                  coordinate system and viewport. It is used as the outermost
                  element of SVG documents, but it can also be used to embed
                  an SVG fragment inside an SVG or HTML document."]
   :attrs [:overflow]})

(defn shadow
  [rect-attrs shadow-size transform]
  (let [offset (str "translate(" shadow-size " " shadow-size ")")]
    [:rect (merge rect-attrs
                  {:fill "rgba(0, 0, 0, .1)"
                   :transform (if transform (str offset " " transform) offset)
                   :style {:filter (str "blur(" shadow-size "px)")}})]))

(defn viewport-rect
  [el]
  (let [[a _b _c d e f] (utils.element/transform el)
        [x y w h] (utils.element/viewbox el)]
    [(/ (- x e) a)
     (/ (- y f) d)
     (/ w a)
     (/ h d)]))

(defn render-label
  [{:keys [el label-anchor pointer-handler zoom]}]
  (let [label-transform (-> (utils.element/transform-attr el)
                            (utils.transform/unscale-at label-anchor)
                            (utils.transform/->str))]
    [:text
     (merge
      (zipmap [:x :y] label-anchor)
      {:on-pointer-up pointer-handler
       :on-pointer-down pointer-handler
       :on-pointer-move pointer-handler
       :fill "var(--foreground-muted)"
       :font-size (/ 12 zoom)
       :transform label-transform})
     (or (:label el)
         (i18n.views/t label))]))

(defmethod element.hierarchy/render :svg
  [_el]
  (let [ref (react/createRef)]
    (reagent/create-class
     {:display-name "svg-renderer"

      :component-did-mount #(some-> ref (.-current) (.pauseAnimations))

      :reagent-render
      (fn [el]
        (let [{:keys [selected attrs children]} el
              child-els @(rf/subscribe [::element.subs/filter-visible children])
              rect-attrs (select-keys attrs [:x :y :width :height])
              active-filter @(rf/subscribe [::a11y.subs/active-filter])
              zoom @(rf/subscribe [::document.subs/zoom])
              label-anchor [(:x rect-attrs) (- (:y rect-attrs) (/ 10 zoom))]
              pointer-handler (partial input.impl.pointer/handler! el)
              shadow-size (/ 2 zoom)
              [x y w h] (viewport-rect el)]
          [:g
           [render-label {:el el
                          :label-anchor label-anchor
                          :pointer-handler pointer-handler
                          :zoom zoom}]

           [shadow rect-attrs shadow-size (:transform attrs)]

           [:svg
            (cond-> attrs
              :always
              (-> (dissoc :style)
                  (assoc :ref ref))

              active-filter
              (assoc :filter (str "url(#" (name active-filter) ")")))
            [:rect
             {:x x
              :y y
              :width w
              :height h
              :fill "white"
              :on-pointer-up pointer-handler
              :on-pointer-move #(when selected (pointer-handler %))
              :on-pointer-down #(when (or selected (= (.-button %) 2))
                                  (pointer-handler %))}]
            (for [el child-els]
              ^{:key (:id el)}
              [element.hierarchy/render el])]]))})))
