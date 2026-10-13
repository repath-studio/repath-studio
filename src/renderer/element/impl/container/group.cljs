(ns renderer.element.impl.container.group
  "https://www.w3.org/TR/SVG/struct.html#GElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/g"
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.hierarchy :as hierarchy]
   [renderer.input.impl.pointer :as input.impl.pointer]
   [renderer.tool.impl.base.transform.core :as-alias tool.impl.base.transform]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.element :as utils.element]
   [renderer.utils.transform :as utils.transform]))

(hierarchy/derive! :g ::element.hierarchy/container)

(defmethod element.hierarchy/properties :g
  []
  {:icon "group"
   :label [::label "Group"]
   :description [::description "The <g> SVG element is a container used to group
                                other SVG elements."]
   :attrs []})

(defmethod element.hierarchy/render :g
  [el]
  (let [{:keys [attrs children id bbox]} el
        child-els @(rf/subscribe [::element.subs/filter-visible children])]
    [:g (utils.element/style->map attrs)
     (for [child child-els]
       ^{:key (:id child)}
       [element.hierarchy/render child])
     (let [ignored-ids @(rf/subscribe [::document.subs/ignored-ids])
           ignored? (contains? ignored-ids (:id el))
           pointer-handler (partial input.impl.pointer/handler! el)
           handle-size @(rf/subscribe [::document.subs/handle-size])
           transform @(rf/subscribe [::element.subs/transform id])
           stroke-width (/ handle-size (utils.transform/scale-factor transform))
           transform? @(rf/subscribe [::tool.subs/active?
                                      ::tool.impl.base.transform/transform])]
       (when (and transform? bbox)
         (let [[min-x min-y max-x max-y] bbox
               corners [[min-x min-y] [max-x min-y]
                        [max-x max-y] [min-x max-y]]
               world->local (partial utils.transform/inverse-point transform)
               points (->> corners
                           (map (comp (fn [[x y]] (str x "," y)) world->local))
                           (string/join " "))]
           [:polygon {:points points
                      :fill "transparent"
                      :stroke "transparent"
                      :stroke-width stroke-width
                      :pointer-events (when ignored? "none")
                      :on-pointer-up pointer-handler
                      :on-pointer-down pointer-handler
                      :on-pointer-move pointer-handler}])))]))
