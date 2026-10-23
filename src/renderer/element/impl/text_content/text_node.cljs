(ns renderer.element.impl.text-content.text-node
  "A run of plain character data (an SVG text node) within a text element."
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.effects :as-alias effects]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.dom :as utils.dom]
   [renderer.utils.element :as utils.element]
   [renderer.utils.key :as utils.key]
   [renderer.utils.length :as utils.length]
   [renderer.utils.transform :as utils.transform]))

(hierarchy/derive! :text-node ::element.hierarchy/text-content-child)

(defmethod element.hierarchy/properties :text-node
  []
  {:label [::label "Text data"]
   :description [::description
                 "A run of plain character data of text content elements."]
   :attrs [:content]})

(defmethod element.hierarchy/permitted-content :text-node [_el])

(defmethod element.hierarchy/bbox :text-node [_el _transform & _more])

(defmethod element.hierarchy/render :text-node
  [el]
  (let [content (get-in el [:attrs :content])]
    (if (and (:selected el)
             @(rf/subscribe [::tool.subs/editing?]))
      [:tspan {:style {:visibility "hidden"}} content]
      (get-in el [:attrs :content]))))

(defmethod element.hierarchy/render-to-string :text-node
  [el]
  (get-in el [:attrs :content]))

(defmethod attribute.hierarchy/description
  [::element.hierarchy/element :content]
  []
  [::content-description "The character data of this run of text."])

(defn ->local-bbox
  "tspans store their bbox in world coordinates, measured with real text
   layout. Unprojects it so the edit input lands over the actual glyphs."
  [el transform]
  (when-let [[x1 y1 x2 y2] (:bbox el)]
    (let [[lx1 ly1] (utils.transform/inverse-point transform [x1 y1])
          [lx2 ly2] (utils.transform/inverse-point transform [x2 y2])]
      [lx1 ly1 lx2 ly2])))

(defn input-style
  [attrs fill h]
  (let [{:keys [font-family font-size font-weight font-style]} attrs]
    {:color fill
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
     :font-size (let [font-size-px (utils.length/unit->px font-size)]
                  (if (zero? font-size-px) font-size (str font-size-px "px")))
     :font-weight font-weight}))

(defn get-text
  "Retrieves the input value and replaces spaces with no-break space to maintain
   user intent."
  [e]
  (string/replace (.. e -target -value) " " "\u00a0"))

(defn text-metrics
  [font-family font-size]
  (when-let [svg (utils.dom/get-canvas-element)]
    (let [text (.createElementNS js/document utils.dom/svg-ns "text")]
      (.setAttribute text "y" "100")
      (.setAttribute text "font-size" (str font-size))
      (.setAttribute text "font-family" (or font-family "var(--sans)"))
      (.appendChild text (.createTextNode js/document "Hgjpqy"))
      (.appendChild svg text)
      (let [bbox (.getBBox text)]
        (.remove text)
        [(- 100 (.-y bbox)) (.-height bbox)]))))

(defn edit-xy
  "The position to anchor the editor at: el's own x/y, else the nearest text
   container ancestor's, since text data elements carry no position of their
   own."
  [el resolve-child]
  (let [text-el-chain (take-while utils.element/text-content?
                                  (iterate #(some-> % :parent resolve-child)
                                           el))
        attrs-chain (cons (:attrs el) (map :attrs text-el-chain))]
    (or (some (fn [{:keys [x y]}] (when (and (some? x) (some? y)) [x y]))
              attrs-chain)
        [0 0])))

(defn edit-metrics
  [el el-bbox [ascent box-height] resolve-child]
  (let [bbox-h (and el-bbox (second (utils.bounds/->dimensions el-bbox)))]
    (if (pos? bbox-h)
      [(take 2 el-bbox) bbox-h]
      (let [[x y] (edit-xy el resolve-child)]
        [[x (- y ascent)] box-height]))))

(defn resolve-children
  "A resolver of element ids against the current entities subscription."
  []
  (let [by-id (group-by :id @(rf/subscribe [::element.subs/entities]))]
    (fn [cid] (first (get by-id cid)))))

(defn style-of
  [el]
  (merge (dissoc (:attrs el) :style)
         (some-> el :attrs :style utils.element/style-attr->map)))

(defn inherited-styles
  "Text content style resolved up the tspan/text ancestor chain (closest
   wins) over the tag defaults, since SVG text children inherit font and fill
   styles from their ancestors."
  [el resolve-child]
  (merge (utils.attribute/defaults-memo (:tag el))
         (loop [e el acc (style-of el)]
           (let [parent (some-> e :parent resolve-child)]
             (if (contains? #{:text :tspan :textPath} (:tag parent))
               (recur parent (merge (style-of parent) acc))
               acc)))))

(defn text-input
  "Inline editor input overlaid on a text content element, committing its
   value via `set-text-event`."
  [el content resolve-child set-text-event]
  (let [{:keys [id tag]} el
        transform @(rf/subscribe [::element.subs/transform id])
        el-bbox (if (contains? #{:tspan :text-node} tag)
                  (->local-bbox el transform)
                  (utils.element/local-bbox el resolve-child))
        attrs (inherited-styles el resolve-child)
        {:keys [fill font-family font-size]} attrs
        font-size-px (utils.length/unit->px font-size)
        fs (if (pos? font-size-px) font-size-px 16)
        metrics (or (text-metrics font-family fs) [0 fs])
        [[x y] h] (edit-metrics el el-bbox metrics resolve-child)]
    [:foreignObject {:x x
                     :y y
                     :width "1000vw"
                     :height h
                     :transform (utils.transform/->str transform)}
     [:input
      {:key id
       :default-value content
       :auto-focus true
       :enter-key-hint "done"
       :on-focus #(.. % -target select)
       :on-pointer-down #(.stopPropagation %)
       :on-pointer-up #(.stopPropagation %)
       :on-blur #(rf/dispatch [set-text-event id (get-text %)])
       :on-key-down #(utils.key/down-handler % content identity id)
       :ref (fn [this]
              (when this
                (rf/dispatch [::tool.events/set-state :type])))
       :style (input-style attrs fill h)}]]))

(defmethod element.hierarchy/render-edit :text-node
  [el]
  (text-input el
              (get-in el [:attrs :content] "")
              (resolve-children)
              ::set-text))

(defn set-text-node
  [db id s now]
  (if (empty? s)
    (-> (element.handlers/delete db id)
        (history.handlers/finalize now [::remove-text "Remove text"]))
    (-> (element.handlers/update-el db id assoc-in [:attrs :content] s)
        (history.handlers/finalize now [::set-text "Set text"]))))

(rf/reg-event-fx
 ::set-text
 [(rf/inject-cofx ::effects/now)]
 (fn [{:keys [db now]} [_ id s]]
   {:db (-> (set-text-node db id s now)
            (tool.handlers/deactivate))
    ::effects/focus-canvas nil}))
