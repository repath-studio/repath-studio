(ns renderer.element.impl.custom.guide
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.input.impl.pointer :as input.impl.pointer]
   [renderer.tool.views :as tool.views]))

(element.hierarchy/derive-element :guide ::element.hierarchy/renderable)

(defmethod element.hierarchy/properties :guide
  []
  {:icon "ruler-straight"
   :label [::label "Guide"]
   :description [::element-description
                 "The <guide> element is used to create guides for aligning
                  other elements."]
   :attrs [:x
           :y
           :orientation]})

(defmethod element.hierarchy/translate :guide
  [el [x y]]
  (if (= (-> el :attrs :orientation) "vertical")
    (attribute.hierarchy/update-attr el :x + x)
    (attribute.hierarchy/update-attr el :y + y)))

(defn default-attrs
  [attrs viewbox-bounds]
  (let [{:keys [x y orientation]} attrs
        [b-x b-y b-w b-h] viewbox-bounds
        vertical (= orientation "vertical")]
    {:x1 (if vertical x b-x)
     :y1 (if vertical b-y y)
     :x2 (if vertical x b-w)
     :y2 (if vertical b-h y)}))

(defmethod element.hierarchy/render :guide
  [el]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        hovered? @(rf/subscribe [::element.subs/hovered? (:id el)])
        pointer-handler (partial input.impl.pointer/handler! el)
        viewbox-bounds @(rf/subscribe [::frame.subs/viewbox-bounds])
        attrs (default-attrs (:attrs el) viewbox-bounds)]
    [:g
     [:line (merge attrs
                   {:on-pointer-up pointer-handler
                    :on-pointer-down pointer-handler
                    :on-pointer-move pointer-handler
                    :pointer-events "all"
                    :shape-rendering "optimizeSpeed"
                    :stroke "transparent"
                    :stroke-width (/ 5 zoom)})]

     (when hovered?
       [:line (merge attrs {:pointer-events "none"
                            :stroke "white"
                            :stroke-width (/ 1 zoom)})])

     [:line (merge attrs
                   {:pointer-events "none"
                    :stroke-width (/ 1 zoom)
                    :stroke-dasharray (when hovered? (/ 5 zoom))
                    :stroke (if (:selected el)
                              "var(--accent)"
                              "DodgerBlue")})]]))

(defmethod element.hierarchy/edit :guide
  [el [x y] handle _lock?]
  (case handle
    :position (-> el
                  (attribute.hierarchy/update-attr :x + x)
                  (attribute.hierarchy/update-attr :y + y))
    el))

(defmethod element.hierarchy/render-edit :guide
  [el]
  (let [{:keys [attrs]} el
        {:keys [x y]} attrs]

    [tool.views/square-handle {:x x
                               :y y
                               :id :position
                               :label [::position "Position"]
                               :type :handle
                               :action :edit
                               :element-id (:id el)}]))

(defmethod attribute.hierarchy/description [:guide :orientation]
  []
  [::orientation-description "The orientation of the guide."])

(defmethod attribute.hierarchy/form-element [:guide :orientation]
  [_ k v attrs]
  [attribute.views/select-input k v
   (merge attrs
          {:default-value "vertical"
           :items [{:id :vertical
                    :label [::normal "Vertical"]
                    :value "vertical"}
                   {:id :horizontal
                    :label [::italic "Horizontal"]
                    :value "horizontal"}]})])

(defmethod attribute.hierarchy/initial [:guide :orientation] [] "vertical")
