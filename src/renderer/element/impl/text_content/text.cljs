(ns renderer.element.impl.text-content.text
  "https://www.w3.org/TR/SVG/text.html
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/text"
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.impl.text-content.text-node]
   [renderer.element.impl.text-content.tspan]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.hierarchy :as hierarchy]
   [renderer.input.impl.pointer :as input.impl.pointer]
   [renderer.tool.impl.base.transform.core :as-alias tool.impl.base.transform]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.element :as utils.element]
   [renderer.utils.font :as utils.font]
   [renderer.utils.length :as utils.length]
   [renderer.utils.transform :as utils.transform]))

(hierarchy/derive! :text ::element.hierarchy/text-content)

(defmethod element.hierarchy/properties :text
  []
  {:icon "text"
   :label [::label "Text"]
   :description [::description
                 "The SVG <text> element draws a graphics element consisting
                  of text. It's possible to apply a gradient, pattern,
                  clipping path, mask, or filter to <text>, like any other
                  SVG graphics element."]
   :attrs [:font-family
           :font-size
           :font-weight
           :font-style
           :stroke
           :stroke-width
           :stroke-dasharray
           :opacity]})

(defn content-of
  [el resolve-child]
  (let [resolve-child (or resolve-child (constantly nil))]
    (letfn [(collect [e]
              (if (utils.element/text-node? e)
                (or (get-in e [:attrs :content]) "")
                (string/join
                 (map collect
                      (keep resolve-child (or (:children e) []))))))]
      (string/join
       (map collect (keep resolve-child (or (:children el) [])))))))

(defmethod element.hierarchy/path :text
  [el & more]
  (let [resolve-child (first more)
        content (content-of el resolve-child)
        {:keys [attrs]} el
        {:keys [x y font-family]} attrs
        computed-styles (utils.element/get-computed-styles el resolve-child)
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

(defn text-descendants
  [el resolve-child]
  (letfn [(walk [e depth]
            (mapcat (fn [cid]
                      (when-let [child (resolve-child cid)]
                        (when (contains? #{:tspan :textPath :text-node}
                                         (:tag child))
                          (cons [child depth] (walk child (inc depth))))))
                    (:children e)))]
    (->> (walk el 0)
         (sort-by (comp - second))
         (map first))))

(defn text-ghost
  [el transform]
  (let [{:keys [id bbox]} el
        ignored-ids @(rf/subscribe [::document.subs/ignored-ids])
        ignored? (contains? ignored-ids id)
        pointer-handler (partial input.impl.pointer/handler! el)
        handle-size @(rf/subscribe [::document.subs/handle-size])
        stroke-width (/ handle-size (utils.transform/scale-factor transform))
        idle? @(rf/subscribe [::tool.subs/idle?])
        transform? @(rf/subscribe [::tool.subs/active?
                                   ::tool.impl.base.transform/transform])
        clickable? (and bbox (or idle? transform?))]
    (when clickable?
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
                   :on-pointer-move pointer-handler}]))))

(defn text-ghosts
  [el transform]
  (let [entities @(rf/subscribe [::element.subs/entities])
        by-id (group-by :id entities)
        resolve-child (fn [cid] (first (get by-id cid)))]
    (->> (concat (text-descendants el resolve-child) [el])
         (map (fn [el] ^{:key (:id el)} [text-ghost el transform])))))

(defmethod element.hierarchy/render :text
  [el]
  (let [child-els @(rf/subscribe [::element.subs/filter-visible (:children el)])
        idle? @(rf/subscribe [::tool.subs/idle?])
        editing? @(rf/subscribe [::tool.subs/editing?])
        text? (= (:tag el) :text)
        transform @(rf/subscribe [::element.subs/transform (:parent el)])]
    [:g {:cursor (when editing? "text")}
     [element.views/render-to-dom el child-els idle?]
     (when text? (text-ghosts el transform))]))
