(ns renderer.element.impl.text
  "https://www.w3.org/TR/SVG/text.html
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/text"
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.effects :as-alias effects]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.font :as utils.font]
   [renderer.utils.key :as utils.key]
   [renderer.utils.length :as utils.length]))

(element.hierarchy/derive-element :text ::element.hierarchy/shape)

(defmethod element.hierarchy/properties :text
  []
  {:icon "text"
   :label [::label "Text"]
   :description [::description
                 "The SVG <text> element draws a graphics element consisting
                  of text. It's possible to apply a gradient, pattern,
                  clipping path, mask, or filter to <text>, like any other
                  SVG graphics element."]
   :ratio-locked true
   :attrs [:font-family
           :font-size
           :font-weight
           :font-style
           :fill
           :stroke
           :stroke-width
           :opacity]})

(defmethod element.hierarchy/translate :text
  [el [x y]]
  (-> el
      (attribute.hierarchy/update-attr :x + x)
      (attribute.hierarchy/update-attr :y + y)))

(defmethod element.hierarchy/scale :text
  [el ratio pivot-point]
  (let [bounds (element.hierarchy/bbox el)
        [_w h] (utils.bounds/->dimensions bounds)
        pivot-point (matrix/sub pivot-point [0 (/ h 2)])
        offset (utils.element/scale-offset ratio pivot-point)
        ratio (apply min ratio)]
    (-> el
        (attribute.hierarchy/update-attr :font-size * ratio)
        (element.hierarchy/translate offset))))

(defn get-text!
  "Retrieves the input value and replaces spaces with no-break space to maintain
   user intent."
  [e]
  (string/replace (.. e -target -value) " " "\u00a0"))

(rf/reg-event-fx
 ::set-text
 (fn [{:keys [db]} [_ id s]]
   {:db (-> (if (empty? s)
              (-> (element.handlers/delete db id)
                  (history.handlers/finalize [::remove-text "Remove text"]))
              (-> (element.handlers/assoc-prop db id :content s)
                  (element.handlers/refresh-bbox id)
                  (history.handlers/finalize [::set-text "Set text"])))
            (tool.handlers/activate :transform))
    ::effects/focus-canvas nil}))

(defmethod element.hierarchy/render :text
  [el]
  (let [child-els @(rf/subscribe [::element.subs/filter-visible (:children el)])
        state @(rf/subscribe [::tool.subs/state])
        tool @(rf/subscribe [::tool.subs/active])]
    (when-not (and (= tool :edit) (:selected el))
      [element.views/render-to-dom el child-els (= state :idle)])))

(defmethod element.hierarchy/render-edit :text
  [el]
  (let [{:keys [id content]} el
        offset (utils.element/offset el)
        el-bbox (element.hierarchy/bbox el)
        [x y] (matrix/add (take 2 el-bbox) offset)
        [_w h] (utils.bounds/->dimensions el-bbox)
        attrs (utils.element/attributes el)
        {:keys [fill font-family font-size font-weight font-style]} attrs
        font-size-px (utils.length/unit->px font-size)
        font-size (if (zero? font-size-px)
                    font-size
                    (str font-size-px "px"))]
    [:foreignObject {:x x
                     :y y
                     :width "1000vw"
                     :height h}
     [:input
      {:key id
       :default-value content
       :auto-focus true
       :enter-key-hint "done"
       :on-focus #(.. % -target select)
       :on-pointer-down #(.stopPropagation %)
       :on-pointer-up #(.stopPropagation %)
       :on-blur #(rf/dispatch [::set-text id (get-text! %)])
       :on-key-down #(utils.key/down-handler! % content identity id)
       :ref (fn [this]
              (when this
                (rf/dispatch [::tool.events/set-state :type])))
       :style {:color fill
               :caret-color fill
               :display "block"
               :width "1000vw"
               :height h
               :padding 0
               :border 0
               :outline "none"
               :background "transparent"
               :font-style font-style
               :font-family (if (empty? font-family) "var(--sans)" font-family)
               :font-size font-size
               :font-weight font-weight}}]]))

(defmethod element.hierarchy/path :text
  [el]
  (let [{:keys [attrs content]} el
        {:keys [x y font-family]} attrs
        computed-styles (utils.font/get-computed-styles! el)
        {:keys [font-size font-style font-weight]} computed-styles
        [x y font-size] (mapv utils.length/unit->px [x y font-size])]
    (if font-family
      (some-> (.-queryLocalFonts js/window)
              (.call)
              (.then (fn [fonts]
                       (some-> fonts
                               (utils.font/match-font font-family
                                                      font-style
                                                      font-weight)
                               (utils.font/font-data->path-data! content
                                                                 x y
                                                                 font-size)))))
      (-> (utils.font/default-font-path font-style font-weight)
          (js/fetch)
          (.then #(utils.font/font-data->path-data! % content
                                                    x y
                                                    font-size))))))

(defmethod element.hierarchy/bbox :text
  [el]
  (:bbox (utils.font/get-computed-styles! el)))
