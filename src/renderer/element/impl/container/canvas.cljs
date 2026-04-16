(ns renderer.element.impl.container.canvas
  "The main SVG element that hosts all pages."
  (:require
   [re-frame.core :as rf]
   [renderer.a11y.subs :as-alias a11y.subs]
   [renderer.app.subs :as-alias app.subs]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.input.impl.drag :as input.impl.drag]
   [renderer.input.impl.keyboard :as input.impl.keyboard]
   [renderer.input.impl.pointer :as input.impl.pointer]
   [renderer.ruler.views :as ruler.views]
   [renderer.snap.subs :as-alias snap.subs]
   [renderer.snap.views :as snap.views]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.svg :as utils.svg]))

(element.hierarchy/derive-element
 :canvas
 ::element.hierarchy/element)

(defmethod element.hierarchy/properties :canvas
  []
  {:label [::label "Canvas"]
   :description [::description "The canvas is the main SVG container that hosts
                                all elements."]
   :attrs [:fill]})

(defmethod attribute.hierarchy/initial [:canvas :fill] [] "")

(defn a11y-filter
  [{:keys [id tag attrs]}]
  [:filter {:id id
            :key id}
   [tag attrs]])

(defn snap-info
  []
  (let [nearest-neighbor @(rf/subscribe [::snap.subs/nearest-neighbor])
        snapped-el-id (-> nearest-neighbor meta :id)
        snapped-el @(rf/subscribe [::element.subs/entity snapped-el-id])]
    (when snapped-el
      [utils.svg/bounding-box (:bbox snapped-el) true])

    (when nearest-neighbor
      [snap.views/canvas-label nearest-neighbor])))

(defmethod element.hierarchy/render :canvas
  [el]
  (let [{:keys [attrs children]} el
        child-elements @(rf/subscribe [::element.subs/filter-visible children])
        viewbox-attr @(rf/subscribe [::frame.subs/viewbox-attr])
        {:keys [width height]} @(rf/subscribe [::app.subs/dom-rect])
        read-only? @(rf/subscribe [::document.subs/read-only?])
        cursor @(rf/subscribe [::tool.subs/cursor])
        active-tool @(rf/subscribe [::tool.subs/active])
        cached-tool @(rf/subscribe [::tool.subs/cached])
        rotate @(rf/subscribe [::document.subs/rotate])
        grid? @(rf/subscribe [::app.subs/grid?])
        state @(rf/subscribe [::tool.subs/state])
        pointer-handler (partial input.impl.pointer/handler! el)
        filters @(rf/subscribe [::a11y.subs/filters])
        snap? @(rf/subscribe [::snap.subs/active?])]
    [:svg#canvas {:on-pointer-up pointer-handler
                  :on-pointer-down pointer-handler
                  :on-pointer-move pointer-handler
                  :on-key-up input.impl.keyboard/handler!
                  :on-key-down input.impl.keyboard/handler!
                  :tab-index 0 ; Enable keyboard events
                  :viewBox viewbox-attr
                  :on-drop input.impl.drag/handler!
                  :on-drag-over input.impl.drag/handler!
                  :width width
                  :height height
                  :transform (str "rotate(" rotate ")")
                  :cursor cursor
                  :style {:outline 0
                          :background (:fill attrs)}}
     (for [el child-elements]
       ^{:key (:id el)}
       [element.hierarchy/render el])

     (->> filters
          (map a11y-filter)
          (into [:defs]))

     (when grid?
       [ruler.views/grid])

     (when (and snap? (not= state :select))
       [snap-info])

     (when-not read-only?
       [tool.hierarchy/render (or cached-tool active-tool)])]))

(defmethod element.hierarchy/render-to-string :canvas
  [el]
  (let [{:keys [attrs children]} el
        child-elements @(rf/subscribe [::element.subs/filter-visible children])
        attrs (->> (dissoc attrs :fill)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (into [:svg attrs]
          (map element.hierarchy/render-to-string)
          child-elements)))
