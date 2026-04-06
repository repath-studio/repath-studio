(ns renderer.element.views
  (:require
   ["react" :as react]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.event.impl.pointer :as event.impl.pointer]))

(defn ghost-element
  "Renders a ghost element on top of the actual element to ensure that the user
   can interact with it."
  [el]
  (let [{:keys [attrs tag content]} el
        pointer-handler (partial event.impl.pointer/handler! el)
        zoom @(rf/subscribe [::document.subs/zoom])
        stroke-width (max (:stroke-width attrs) (/ 20 zoom))]
    [tag
     (merge (dissoc attrs :style)
            {:on-pointer-up pointer-handler
             :on-pointer-down pointer-handler
             :on-pointer-move pointer-handler
             :shape-rendering "optimizeSpeed"
             :fill "transparent"
             :stroke "transparent"
             :stroke-width stroke-width})
     content]))

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
          (some-> (.-pauseAnimations dom-el)
                  (.call))
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
        (let [{:keys [attrs tag title content]} el]
          [:<>
           [tag (-> attrs
                    (dissoc :style)
                    (assoc :shape-rendering "geometricPrecision"
                           :ref ref))
            (when title [:title title])
            content
            (for [child child-els]
              ^{:key (:id child)}
              [element.hierarchy/render child])]

           (when idle [ghost-element el])]))})))
