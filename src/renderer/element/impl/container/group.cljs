(ns renderer.element.impl.container.group
  "https://www.w3.org/TR/SVG/struct.html#GElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/g"
  (:require
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.hierarchy :as hierarchy]
   [renderer.input.impl.pointer :as input.impl.pointer]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]))

(hierarchy/derive! :g ::element.hierarchy/container)

(defmethod element.hierarchy/properties :g
  []
  {:icon "group"
   :label [::label "Group"]
   :description [::description "The <g> SVG element is a container used to group
                                other SVG elements."]
   :attrs [:transform]})

(defmethod element.hierarchy/render :g
  [el]
  (let [{:keys [attrs children id bbox]} el
        child-els @(rf/subscribe [::element.subs/filter-visible children])

        parent-offset @(rf/subscribe [::element.subs/parent-offset id])]
    [:g (utils.element/style->map attrs)
     (for [child child-els]
       ^{:key (:id child)}
       [element.hierarchy/render child])
     (let [ignored-ids @(rf/subscribe [::document.subs/ignored-ids])
           ignored? (contains? ignored-ids (:id el))
           [min-x min-y] (matrix/sub (take 2 bbox) parent-offset)
           [w h] (utils.bounds/->dimensions bbox)
           pointer-handler (partial input.impl.pointer/handler! el)
           handle-size @(rf/subscribe [::document.subs/handle-size])
           stroke-width (max (:stroke-width attrs) handle-size)]
       [:rect {:x min-x
               :y min-y
               :width w
               :height h
               :fill "transparent"
               :stroke "red"
               :stroke-width stroke-width
               :pointer-events (when ignored? "none")
               :on-pointer-up pointer-handler
               :on-pointer-down pointer-handler
               :on-pointer-move pointer-handler}])]))
