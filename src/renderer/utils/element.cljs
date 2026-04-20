(ns renderer.utils.element
  (:require
   ["paper" :refer [Path]]
   ["paperjs-offset" :refer [PaperOffset]]
   ["style-to-object" :default parse]
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [malli.core :as m]
   [reagent.dom.server :as dom.server]
   [renderer.db :refer [BBox Vec2 JS_Element]]
   [renderer.element.db :as element.db :refer [Element ElementAttrs]]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.snap.db :refer [SnapOptions]]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.map :as utils.map]))

(m/=> root? [:-> Element boolean?])
(defn root?
  [el]
  (= :canvas (:tag el)))

(m/=> svg? [:-> Element boolean?])
(defn svg?
  [el]
  (= :svg (:tag el)))

(m/=> container? [:-> Element boolean?])
(defn container?
  [el]
  (or (svg? el) (root? el)))

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

(m/=> united-bbox [:-> [:sequential Element] [:maybe BBox]])
(defn united-bbox
  [elements]
  (let [el-bbox (keep :bbox elements)]
    (when (seq el-bbox)
      (apply utils.bounds/union el-bbox))))

(m/=> offset [:-> Element Vec2])
(defn offset
  [el]
  (let [el-bbox (:bbox el)
        local-bbox (element.hierarchy/bbox el)]
    (into [] (take 2) (matrix/sub el-bbox local-bbox))))

(m/=> acc-snapping-points [:-> Element SnapOptions [:* Vec2]])
(defn acc-snapping-points
  [el options]
  (let [points (or (when (contains? options :nodes)
                     (mapv #(with-meta
                              (matrix/add % (offset el))
                              (merge (meta %) {:id (:id el)}))
                           (element.hierarchy/snapping-points el)))
                   [])]
    (cond-> points
      (:bbox el)
      (into (mapv #(with-meta % (merge (meta %) {:id (:id el)}))
                  (utils.bounds/->snapping-points (:bbox el) options))))))

(m/=> attributes [:-> map? map?])
(defn attributes
  "Returns existing attributes merged with defaults."
  [{:keys [tag attrs]}]
  (cond->> attrs
    tag
    (merge (utils.attribute/defaults-memo tag))))

(m/=> supported-attr? [:-> map? keyword? boolean?])
(defn supported-attr?
  [props k]
  (-> props attributes k boolean))

(m/=> normalize-attr-key [:-> map? keyword? keyword?])
(defn normalize-attr-key
  [props k]
  (cond-> k
    (not (supported-attr? props k))
    utils.attribute/->camel-case-memo))

(m/=> normalize-attrs [:-> map? map?])
(defn normalize-attrs
  [props]
  (-> props
      (update :attrs update-vals str)
      (update :attrs update-keys (partial normalize-attr-key props))))

(m/=> ->path [:-> Element Element])
(defn ->path
  ([el]
   (if (get-method element.hierarchy/path (:tag el))
     (->path el (element.hierarchy/path el))
     (js/Promise.reject (str "No path implementation for " (name (:tag el))
                             " elements."))))
  ([el d]
   (let [default-attrs (utils.attribute/defaults-memo :path)]
     (cond
       (string? d)
       (-> (assoc el :tag :path)
           (update :attrs #(utils.map/merge-common-with str % default-attrs))
           (assoc-in [:attrs :d] d))

       (instance? js/Promise d)
       (.then d (partial ->path el))

       :else
       el))))

(m/=> stroke->path [:-> Element Element])
(defn stroke->path
  [{:keys [attrs]
    :as el}]
  (let [{:keys [d stroke stroke-width stroke-linecap stroke-linejoin]} attrs
        paper-path (Path. d)
        el-offset (or stroke-width 1)
        stroke-path (PaperOffset.offsetStroke
                     paper-path
                     (/ el-offset 2)
                     #js {:cap (or stroke-linecap "butt")
                          :join (or stroke-linejoin "miter")})
        new-d (.getAttribute (.exportSVG stroke-path) "d")
        default-attrs (utils.attribute/defaults-memo :path)]
    (-> (assoc el :tag :path)
        (update :attrs dissoc :stroke :stroke-width)
        (update :attrs #(utils.map/merge-common-with str % default-attrs))
        (assoc-in [:attrs :d] new-d)
        (assoc-in [:attrs :fill] stroke))))

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
        [w h] (utils.bounds/->dimensions bbox)
        viewbox (string/join " " [min-x min-y w h])]
    (->string [{:tag :svg
                :children (mapv :id els)
                :attrs {:width (str w)
                        :height (str h)
                        :viewBox viewbox
                        :xmlns "http://www.w3.org/2000/svg"}}])))

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

(m/=> scale-offset [:-> Vec2 Vec2 Vec2])
(defn scale-offset
  [ratio pivot-point]
  (->> ratio
       (matrix/mul pivot-point)
       (matrix/sub pivot-point)))

(m/=> ->dom-element [:-> Element JS_Element])
(defn ->dom-element
  [el]
  (let [{:keys [tag attrs]} el
        dom-el (->> (name tag)
                    (js/document.createElementNS "http://www.w3.org/2000/svg"))
        el (dissoc el :attrs)
        supported-attrs (->> attrs
                             (keep (fn [[k v]]
                                     (when (supported-attr? el k)
                                       [k v]))))]
    (doseq [[k v] supported-attrs]
      (.setAttributeNS dom-el nil (name k) v))
    dom-el))

(m/=> normalize [:-> map? Element])
(defn normalize
  [props]
  (cond-> props
    (not (string? (:content props)))
    (dissoc :content)

    :always
    (-> (utils.map/remove-nils)
        (normalize-attrs)
        (dissoc :locked)
        (merge element.db/default))))
