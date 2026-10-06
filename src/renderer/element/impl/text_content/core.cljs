(ns renderer.element.impl.text-content.core
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.effects :as-alias effects]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.impl.text-content.text]
   [renderer.element.impl.text-content.tspan]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.font :as utils.font]
   [renderer.utils.key :as utils.key]
   [renderer.utils.length :as utils.length]))

(hierarchy/derive! ::element.hierarchy/text-content ::element.hierarchy/shape)
(hierarchy/derive! ::element.hierarchy/text-content-child
                   ::element.hierarchy/text-content)

(defmethod element.hierarchy/permitted-content ::element.hierarchy/text-content
  [_el]
  #{::element.hierarchy/animation
    ::element.hierarchy/descriptive
    ::element.hierarchy/text-content-child
    :a})

(defmethod element.hierarchy/translate ::element.hierarchy/text-content
  [el [x y]]
  (-> el
      (attribute.hierarchy/update-attr :x + x)
      (attribute.hierarchy/update-attr :y + y)))

(defmethod element.hierarchy/scale ::element.hierarchy/text-content
  [el ratio pivot-point]
  (let [bounds (element.hierarchy/bbox el)
        [w h] (utils.bounds/->dimensions bounds)
        y-attr (utils.length/unit->px (get-in el [:attrs :y]))
        ascent (- y-attr (second bounds))
        descent (- h ascent)
        pivot-point (matrix/sub pivot-point [0 ascent])
        [offset-x offset-y] (utils.element/scale-offset ratio pivot-point)
        ratio (apply min ratio)
        offset [(+ offset-x (min 0 (* w ratio)))
                (+ offset-y (* (- (min 0 ratio)) (- ascent descent)))]]
    (-> el
        (attribute.hierarchy/update-attr :font-size #(abs (* % ratio)))
        (element.hierarchy/translate offset))))

(defn get-text
  "Retrieves the input value and replaces spaces with no-break space to maintain
   user intent."
  [e]
  (string/replace (.. e -target -value) " " "\u00a0"))

(rf/reg-event-fx
 ::set-text
 [(rf/inject-cofx ::effects/now)]
 (fn [{:keys [db now]} [_ id s]]
   {:db (-> (if (and (empty? s)
                     (empty? (:children (element.handlers/entity db id))))
              (-> (element.handlers/delete db id)
                  (history.handlers/finalize now [::remove-text "Remove text"]))
              (-> (element.handlers/assoc-prop db id :content s)
                  (element.handlers/refresh-bbox id)
                  (history.handlers/finalize now [::set-text "Set text"])))
            (tool.handlers/deactivate))
    ::effects/focus-canvas nil}))

(defmethod element.hierarchy/render-edit ::element.hierarchy/text-content
  [el]
  (let [{:keys [id content]} el
        el-bbox (element.hierarchy/bbox el)
        [x y] el-bbox
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
       :on-blur #(rf/dispatch [::set-text id (get-text %)])
       :on-key-down #(utils.key/down-handler % content identity id)
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

(defmethod element.hierarchy/path ::element.hierarchy/text-content
  [el]
  (let [{:keys [attrs content]} el
        {:keys [x y font-family]} attrs
        computed-styles (utils.element/get-computed-styles el)
        {:keys [font-size font-style font-weight]} computed-styles
        [x y font-size] (mapv utils.length/unit->px [x y font-size])
        props {:x x
               :y y
               :font-size font-size}]
    (if font-family
      (some-> (.-queryLocalFonts js/window)
              (.call)
              (.then (fn [fonts]
                       (some-> fonts
                               (utils.font/match-font font-family
                                                      font-style
                                                      font-weight)
                               (utils.font/font-data->path-data! content
                                                                 props)))))
      (-> (utils.font/default-font-path font-style font-weight)
          (js/fetch)
          (.then #(utils.font/font-data->path-data! % content props))))))
