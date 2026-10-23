(ns renderer.element.views
  (:require
   ["react" :as react]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.input.impl.pointer :as input.impl.pointer]
   [renderer.utils.transform :as utils.transform]))

(defn ghost-element
  "Renders a ghost element on top of the actual element to ensure that the user
   can interact with it."
  [el pointer-handler]
  (let [{:keys [attrs tag id]} el
        handle-size @(rf/subscribe [::document.subs/handle-size])
        transform @(rf/subscribe [::element.subs/transform id])
        stroke-width (/ (* handle-size 2)
                        (utils.transform/scale-factor transform))]
    [tag (merge (dissoc attrs :style)
                {:on-pointer-up pointer-handler
                 :on-pointer-down pointer-handler
                 :on-pointer-move pointer-handler
                 :shape-rendering "optimizeSpeed"
                 :fill "transparent"
                 :stroke "transparent"
                 :stroke-width stroke-width})]))

(defn render-to-dom
  "We need a reagent form-3 component to set the style attribute manually.
   React expects a map, but we need to set a string to avoid serializing css."
  [el _child-els _idle?]
  (let [ref (react/createRef)]
    (reagent/create-class
     {:display-name "element-renderer"

      :component-did-mount
      (fn
        [_this]
        (let [dom-el (.-current ref)]
          (.setAttribute dom-el "style" (-> el :attrs :style))))

      :component-did-update
      (fn
        [this _]
        (let [new-argv (second (reagent/argv this))
              style (:style (into {} (:attrs (into {} new-argv))))]
          (.setAttribute (.-current ref) "style" style)))

      :reagent-render
      (fn
        [el child-els idle]
        (let [{:keys [attrs tag title]} el
              pointer-handler (partial input.impl.pointer/handler! el)]
          [:<>
           [tag (merge (dissoc attrs :style)
                       {:shape-rendering "geometricPrecision"
                        :ref ref
                        :on-pointer-up pointer-handler
                        :on-pointer-down pointer-handler
                        :on-pointer-move pointer-handler})
            (when title [:title title])
            (for [child child-els]
              ^{:key (:id child)}
              [element.hierarchy/render child])]

           (when idle
             [ghost-element el pointer-handler])]))})))
