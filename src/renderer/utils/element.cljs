(ns renderer.utils.element
  (:require
   ["paper" :refer [Path]]
   ["paperjs-offset" :refer [PaperOffset]]
   ["style-to-object" :as parse]
   [clojure.string :as string]
   [clojure.zip :as zip]
   [hickory.core :as hickory]
   [hickory.zip]
   [malli.core :as m]
   [malli.transform :as m.transform]
   [reagent.dom.server :as dom.server]
   [renderer.db :refer [BBox Vec2 JS_Element TransformationMatrix Viewbox]]
   [renderer.element.db
    :as element.db
    :refer [Element ElementAttrs ElementTag PersistedElement]]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.hierarchy :as hierarchy]
   [renderer.snap.db :refer [SnapOptions]]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.dom :as utils.dom]
   [renderer.utils.length :as utils.length]
   [renderer.utils.map :as utils.map]
   [renderer.utils.math :as utils.math]
   [renderer.utils.transform :as utils.transform]))

(m/=> root? [:-> Element boolean?])
(defn root?
  [el]
  (= :canvas (:tag el)))

(m/=> svg? [:-> map? boolean?])
(defn svg?
  [el]
  (= :svg (:tag el)))

(defn text-node?
  [el]
  (= (:tag el) :text-node))

(defn text-content?
  [el]
  (hierarchy/gisa? (:tag el) :renderer.element.hierarchy/text-content))

(m/=> text-content-child? [:-> map? boolean?])
(defn text-content-child?
  [el]
  (hierarchy/gisa? (:tag el) :renderer.element.hierarchy/text-content-child))

(m/=> transform-attr [:-> Element TransformationMatrix])
(defn transform-attr
  [el]
  (-> (or (utils.transform/parse (get-in el [:attrs :transform])) [])
      (utils.transform/ops->matrix)))

(m/=> viewbox [:-> Element Viewbox])
(defn viewbox
  [el]
  (let [{:keys [x y width height]} (:attrs el)]
    [(utils.length/unit->px x :svg :x)
     (utils.length/unit->px y :svg :y)
     (utils.length/unit->px width :svg :width)
     (utils.length/unit->px height :svg :height)]))

(m/=> view-box->transform [:-> Viewbox Viewbox boolean? TransformationMatrix])
(defn view-box->transform
  [[x y width height] [vb-x vb-y vb-w vb-h] preserve-none?]
  (let [[sx sy] (if preserve-none?
                  [(/ width vb-w) (/ height vb-h)]
                  (let [s (min (/ width vb-w) (/ height vb-h))]
                    [s s]))
        tx (+ x (/ (- width (* sx vb-w)) 2))
        ty (+ y (/ (- height (* sy vb-h)) 2))]
    (-> (utils.transform/translate tx ty)
        (utils.transform/compose (utils.transform/scale [sx sy]))
        (utils.transform/compose (utils.transform/translate (- vb-x)
                                                            (- vb-y))))))

(m/=> transform [:-> Element TransformationMatrix])
(defn transform
  [el]
  (cond
    (svg? el)
    (let [{:keys [viewBox preserveAspectRatio]} (:attrs el)
          [x y width height] (viewbox el)
          [vb-x vb-y vb-w vb-h] (some-> viewBox (utils.attribute/view-box))
          preserve-none? (some-> preserveAspectRatio
                                 (string/trim)
                                 (string/lower-case)
                                 (string/starts-with? "none"))]
      (if-not (every? #(and (pos? %) (js/isFinite %)) [vb-w vb-h width height])
        (utils.transform/translate x y)
        (view-box->transform [x y width height]
                             [vb-x vb-y vb-w vb-h]
                             preserve-none?)))

    :else
    utils.transform/identity-transform))

(m/=> own-transform [:-> Element TransformationMatrix])
(defn own-transform
  [el]
  (if (root? el)
    utils.transform/identity-transform
    (utils.transform/compose (transform-attr el) (transform el))))

(m/=> untransform-point [:-> TransformationMatrix Vec2 Vec2])
(defn untransform-point
  [m point]
  (utils.transform/inverse-point m point))

(defn local-bbox
  ([el]
   (element.hierarchy/bbox el utils.transform/identity-transform))
  ([el resolve-child]
   (element.hierarchy/bbox el
                           utils.transform/identity-transform
                           resolve-child)))

(defn own-bbox
  ([el]
   (-> (transform el)
       (utils.transform/inverse)
       (utils.transform/aabb (local-bbox el))))
  ([el resolve-child]
   (-> (transform el)
       (utils.transform/inverse)
       (utils.transform/aabb (local-bbox el resolve-child)))))

(def properties-memo (memoize element.hierarchy/properties))

(m/=> properties [:-> Element [:maybe map?]])
(defn properties
  [el]
  (-> el :tag properties-memo))

(m/=> ratio-locked? [:-> Element boolean?])
(defn ratio-locked?
  [el]
  (-> el properties :ratio-locked boolean))

(m/=> top-level? [:-> Element boolean?])
(defn top-level?
  [el]
  (-> el properties :top-level boolean))

(m/=> virtual? [:-> Element boolean?])
(defn virtual?
  [el]
  (-> el properties :virtual boolean))

(m/=> united-bbox [:-> [:sequential Element] [:maybe BBox]])
(defn united-bbox
  [els]
  (let [el-bbox (keep :bbox els)]
    (when (seq el-bbox)
      (apply utils.bounds/union el-bbox))))

(m/=> area [:-> [:sequential Element] number?])
(defn area
  [els]
  (reduce #(+ %1 (element.hierarchy/area %2)) 0 els))

(m/=> acc-snapping-points [:->
                           Element TransformationMatrix SnapOptions
                           [:* Vec2]])
(defn acc-snapping-points
  [el el-transform options]
  (let [points (or (when (contains? options :nodes)
                     (let [centroid (element.hierarchy/centroid el)]
                       (mapv #(with-meta
                                (utils.transform/point el-transform %)
                                (merge (meta %) {:id (:id el)}))
                             (cond-> (element.hierarchy/snapping-points el)
                               centroid
                               (conj (with-meta centroid
                                       {:label [::centroid "centroid"]}))))))
                   [])]
    (cond-> points
      (:bbox el)
      (into (mapv #(with-meta % (merge (meta %) {:id (:id el)}))
                  (utils.bounds/->snapping-points (:bbox el) options))))))

(m/=> attributes [:-> map? map?])
(defn attributes
  "Returns existing attributes merged with defaults."
  [el]
  (let [{:keys [tag attrs]} el]
    (cond->> attrs
      tag
      (merge (utils.attribute/defaults-memo tag)))))

(m/=> sorted-attributes [:-> Element [:sequential [:tuple keyword? string?]]])
(defn sorted-attributes
  [el]
  (let [props (properties el)]
    (->> (attributes el)
         (sort-by (fn [[id _]] (some-> props :attrs (.indexOf id)))))))

(m/=> common-attributes [:-> [:sequential Element] ElementAttrs])
(defn common-attributes
  [els]
  (->> els
       (map attributes)
       (apply utils.map/merge-common-with
              (fn [v1 v2] (when (= v1 v2) v1)))))

(m/=> edit-attributes [:->
                       [:sequential Element]
                       [:sequential [:tuple keyword? string?]]])
(defn edit-attributes
  [els]
  (->> (if (second els)
         (-> els common-attributes (dissoc :id))
         (some-> els first sorted-attributes))
       (sort-by (fn [[id _]] (.indexOf utils.attribute/order id)))))

(m/=> supported-attr? [:-> map? keyword? boolean?])
(defn supported-attr?
  [props k]
  (-> props attributes k boolean))

(m/=> base-attr? [:-> ElementTag keyword? boolean?])
(defn base-attr?
  [tag k]
  (-> (utils.attribute/defaults-memo tag)
      (get k)
      (boolean)))

(m/=> normalize-attr-key [:-> map? keyword? keyword?])
(defn normalize-attr-key
  [props k]
  (cond-> k
    (not (base-attr? (:tag props) k))
    utils.attribute/->camel-case-memo))

(m/=> normalize-attrs [:-> map? map?])
(defn normalize-attrs
  [props]
  (-> props
      (update :attrs update-vals str)
      (update :attrs update-keys (partial normalize-attr-key props))))

(m/=> ->path [:-> Element Element])
(defn ->path-with-data
  [el d]
  (let [default-attrs (utils.attribute/defaults-memo :path)]
    (cond
      (string? d)
      (-> (assoc el :tag :path)
          (update :attrs #(utils.map/merge-common-with str % default-attrs))
          (assoc-in [:attrs :d] d)
          (assoc-in [:attrs :transform] (-> el :attrs :transform))
          (update :attrs utils.map/remove-nils))

      (instance? js/Promise d)
      (.then d (partial ->path-with-data el))

      :else
      el)))

(defn ->path
  ([el]
   (->path el nil))
  ([el resolve-child]
   (if (get-method element.hierarchy/path (:tag el))
     (->path-with-data el
                       (if (text-content? el)
                         (element.hierarchy/path el resolve-child)
                         (element.hierarchy/path el)))
     (js/Promise.reject
      (str "No path implementation for " (name (:tag el)) " elements. ")))))

(m/=> stroke->path [:-> Element Element])
(defn stroke->path
  [{:keys [attrs]
    :as el}]
  (let [{:keys [d stroke stroke-width stroke-linecap stroke-linejoin]} attrs
        paper-path (Path. d)
        el-offset (or (utils.length/unit->px stroke-width) 1)
        stroke-path (PaperOffset.offsetStroke
                     paper-path
                     (/ el-offset 2)
                     #js {:cap (or stroke-linecap "butt")
                          :join (or stroke-linejoin "miter")})
        new-d (.getAttribute (.exportSVG stroke-path) "d")
        default-attrs (utils.attribute/defaults-memo :path)]
    (cond-> el
      (seq new-d)
      (-> (assoc :tag :path)
          (update :attrs dissoc :stroke :stroke-width)
          (update :attrs #(utils.map/merge-common-with str % default-attrs))
          (assoc-in [:attrs :d] new-d)
          (assoc-in [:attrs :fill] stroke)
          (assoc-in [:attrs :transform] (:transform attrs))
          (update :attrs utils.map/remove-nils)))))

(m/=> ->string [:-> [:sequential Element] string?])
(defn ->string
  [els]
  (reduce #(-> (element.hierarchy/render-to-string %2)
               (dom.server/render-to-static-markup)
               (str "\n" %)) "" els))

(m/=> ->svg [:-> [:sequential Element] string?])
(defn ->svg
  [els]
  (let [bbox (united-bbox els)
        [min-x min-y _max-x _max-y] bbox
        [w h] (utils.bounds/->dimensions bbox)]
    (->string [{:tag :svg
                :children (mapv :id els)
                :attrs {:width (str w)
                        :height (str h)
                        :viewBox (string/join " " [min-x min-y w h])
                        :xmlns utils.dom/svg-ns}}])))

(m/=> style->map [:-> ElementAttrs ElementAttrs])
(defn style->map
  "Converts :style attribute to map.
   Parsing might throw an exception. When that happens, we remove the attribute
   because there is no other way to handle this gracefully."
  [attrs]
  (try (cond-> (update attrs :style parse)
         (nil? (:style attrs))
         (dissoc :style))
       (catch :default _err (dissoc attrs :style))))

(m/=> style-attr->map [:-> string? ElementAttrs])
(defn style-attr->map
  "Parses a CSS style attribute string into a keyword->value map."
  [s]
  (try (into {} (map (fn [[k v]] [(keyword k) v])) (js->clj (parse s)))
       (catch :default _err {})))

(m/=> scale-offset [:-> Vec2 Vec2 Vec2])
(defn scale-offset
  [ratio pivot-point]
  (->> ratio
       (utils.math/v-mul pivot-point)
       (utils.math/v-sub pivot-point)))

(m/=> ->dom-element [:-> Element JS_Element])
(defn ->dom-element
  [el]
  (let [{:keys [tag attrs]} el
        dom-el (->> (name tag)
                    (js/document.createElementNS utils.dom/svg-ns))
        el (dissoc el :attrs)
        supported-attrs (->> attrs
                             (keep (fn [[k v]]
                                     (when (supported-attr? el k)
                                       [k v]))))]
    (doseq [[k v] supported-attrs]
      (.setAttributeNS dom-el nil (name k) v))
    dom-el))

(m/=> ->dom-subtree [:-> Element any? any?])
(defn ->dom-subtree
  [el resolve-child]
  (let [resolve-child (or resolve-child (constantly nil))]
    (if (text-node? el)
      (.createTextNode js/document (get-in el [:attrs :content]))
      (let [dom-el (->dom-element el)]
        (doseq [child (keep resolve-child (or (:children el) []))]
          (.appendChild dom-el (->dom-subtree child resolve-child)))
        dom-el))))

(defn ->child-dom
  "Builds a temporary DOM subtree from el for measurement, recording each
  tspan and text-node as an [id dom-node] entry appended to acc. Text-nodes
  are wrapped on a tspan so they can be measured. Returns
  [dom-node [child-entries]]."
  [el resolve-child acc]
  (if (text-node? el)
    (let [dom-el (.createElementNS js/document utils.dom/svg-ns "tspan")]
      (when-let [content (get-in el [:attrs :content])]
        (.appendChild dom-el (.createTextNode js/document content)))
      [dom-el (conj acc [(:id el) dom-el])])
    (let [dom-el (->dom-element el)
          acc (if (= (:tag el) :tspan)
                (conj acc [(:id el) dom-el])
                acc)
          acc (reduce (fn [acc child]
                        (let [[c-dom c-acc]
                              (->child-dom child resolve-child acc)]
                          (.appendChild dom-el c-dom)
                          c-acc))
                      acc
                      (keep resolve-child (or (:children el) [])))]
      [dom-el acc])))

(m/=> ->subtree-bboxes [:-> Element any? map?])
(defn ->subtree-bboxes
  "Measures el's subtree with a single DOM insertion, returning
  {:own bbox, :children {child-id bbox}} where :own holds el's own bounds
  and :children holds the bounds of each tspan/text-node descendant, all in
  el's local coordinates."
  [el resolve-child]
  (if-let [svg (utils.dom/get-canvas-element)]
    (let [resolve-child (or resolve-child (constantly nil))
          [dom-el child-acc] (->child-dom el resolve-child [])]
      (.appendChild svg dom-el)
      (let [result {:own (utils.bounds/dom-el->bbox dom-el)
                    :children
                    (into {} (map (fn [[id dom-node]]
                                    [id (utils.bounds/dom-el->bbox dom-node)])
                                  child-acc))}]
        (.remove dom-el)
        result))
    {:own nil
     :children {}}))

(m/=> normalize [:-> map? Element])
(defn normalize
  [props]
  (-> props
      (utils.map/remove-nils)
      (dissoc :content)
      (normalize-attrs)
      (dissoc :locked)
      (merge element.db/default)))

(defn find-svg
  [zipper]
  (loop [loc zipper]
    (cond
      (zip/end? loc)
      (zip/root loc)

      (svg? (zip/node loc))
      (zip/node loc)

      :else
      (recur (zip/next loc)))))

(m/=> persisted [:-> Element PersistedElement])
(defn persisted
  [el]
  (m/decode PersistedElement
            el
            m.transform/strip-extra-keys-transformer))

(m/=> get-computed-styles [:-> Element [:maybe map?]])
(defn get-computed-styles
  ([el]
   (get-computed-styles el nil))
  ([el resolve-child]
   (when-let [svg (utils.dom/get-canvas-element)]
     (let [dom-el (->dom-subtree el resolve-child)]
       (.appendChild svg dom-el)
       (let [computed-style (.getComputedStyle js/window dom-el nil)
             font-style (.getPropertyValue computed-style "font-style")
             font-size (.getPropertyValue computed-style "font-size")
             font-weight (.getPropertyValue computed-style "font-weight")
             bbox (utils.bounds/dom-el->bbox dom-el)]
         (.remove dom-el)
         {:font-style font-style
          :font-size font-size
          :font-weight font-weight
          :bbox bbox})))))

(m/=> breakable? [:-> Element boolean?])
(defn breakable?
  "Returns true if the element has a :d attribute with multiple 'M' commands."
  [el]
  (boolean (some-> el
                   (get-in [:attrs :d])
                   (string/trim)
                   (rest)
                   (str)
                   (string/upper-case)
                   (string/includes? "M"))))

(m/=> transformable? [:-> Element boolean?])
(defn transformable?
  [el]
  (and (not (virtual? el))
       (not (text-content-child? el))))

(m/=> permitted-content? [:-> Element ElementTag boolean?])
(defn permitted-content?
  [parent tag]
  (let [permitted-content (element.hierarchy/permitted-content parent)]
    (->> permitted-content
         (some (partial hierarchy/gisa? tag))
         (boolean))))

(m/=> transformed? [:-> Element boolean?])
(defn transformed?
  [el]
  (not (-> (transform-attr el)
           (utils.transform/identity?))))

(m/=> svg->data [:-> string? [:maybe map?]])
(defn svg->data
  [svg]
  (let [hickory (hickory/as-hickory (hickory/parse svg))
        zipper (hickory.zip/hickory-zip hickory)
        el (find-svg zipper)]
    (when (svg? el)
      (let [el (-> el
                   (update :attrs dissoc :desc :version :xmlns)
                   (assoc :bbox (local-bbox el)))]
        {:elements [el]
         :bbox (:bbox el)}))))
