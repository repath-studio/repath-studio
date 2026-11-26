(ns renderer.snap.handlers
  (:require
   [config :as config]
   [kdtree :as kdtree]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [Vec2]]
   [renderer.element.db :refer [Element ElementId]]
   [renderer.element.handlers :as element.handlers]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.ruler.handlers :as ruler.handlers]
   [renderer.snap.db :refer [AxisMatch NearestNeighbor
                             SnapOption SnapOptions]]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.element :as utils.element]
   [renderer.utils.math :as utils.math]))

(m/=> active? [:-> App boolean?])
(defn active?
  [db]
  (and (:active-document db)
       (-> db :snap :active)))

(m/=> toggle-option [:-> App SnapOption App])
(defn toggle-option
  [db option]
  (let [is-enabled (contains? (-> db :snap :options) option)]
    (update-in db [:snap :options] (if is-enabled disj conj) option)))

(m/=> nearest-neighbors [:-> App [:sequential NearestNeighbor]])
(defn nearest-neighbors
  [db]
  (let [{:keys [viewbox-kdtree]} db]
    (->> (tool.hierarchy/snapping-points db)
         (keep #(some-> (kdtree/nearest-neighbor viewbox-kdtree %)
                        (assoc :base-point %))))))

(m/=> axis-match [:->
                  [:enum :x :y] Vec2 [:* any?] number?
                  [:maybe AxisMatch]])
(defn axis-match
  [axis base-point tree threshold]
  (let [index (case axis :x 0 :y 1)
        base (nth base-point index)]
    (when-let [{:keys [point dist-squared]
                :as result}
               (kdtree/nearest-neighbor tree [base])]
      (when (< dist-squared threshold)
        {:axis axis
         :value (first point)
         :point (-> result meta :point)
         :base-point base-point
         :delta (- (first point) base)
         :dist dist-squared
         :label (-> result meta :label)}))))

(m/=> nearest-axis-match [:->
                          App [:enum :x :y] [:maybe [:sequential Vec2]]
                          number? [:maybe AxisMatch]])
(defn nearest-axis-match
  [db axis base-points threshold]
  (when-let [tree (case axis :x (:x-tree db) :y (:y-tree db))]
    (first
     (sort-by :dist
              (remove nil?
                      (map #(axis-match axis % tree threshold)
                           base-points))))))

(m/=> update-nearest-neighbors [:-> App App])
(defn update-nearest-neighbors
  [db]
  (if-not (active? db)
    db
    (let [zoom (get-in db [:documents (:active-document db) :zoom])
          threshold (Math/pow (/ config/snap-threshold zoom) 2)
          nneighbors (nearest-neighbors db)
          nneighbors (filter #(< (:dist-squared %) threshold) nneighbors)
          nearest (first (sort-by :dist-squared nneighbors))]
      (if nearest
        (-> db
            (assoc :nearest-neighbors nneighbors
                   :nearest-neighbor nearest)
            (dissoc :nearest-x :nearest-y))
        (let [base-points (tool.hierarchy/snapping-points db)]
          (-> db
              (assoc :nearest-neighbors nneighbors)
              (dissoc :nearest-neighbor)
              (assoc :nearest-x
                     (nearest-axis-match db :x base-points threshold))
              (assoc :nearest-y
                     (nearest-axis-match db :y base-points threshold))))))))

(m/=> add-to-cache [:-> App SnapOptions map? Element map?])
(defn add-to-cache
  [db options cache el]
  (let [[cached-points cached-el cached-options] (get cache (:id el))]
    (if (and cached-points
             (= cached-el el)
             (= cached-options options))
      cache
      (let [points (utils.element/acc-snapping-points
                    el (element.handlers/transform db (:id el)) options)]
        (assoc cache (:id el) [points el options])))))

(m/=> update-snapping-points-cache [:->
                                    App [:maybe [:sequential Element]]
                                    App])
(defn update-snapping-points-cache
  [db els]
  (let [options (-> db :snap :options)]
    (assoc db :snapping-points-cache
           (reduce (partial add-to-cache db options)
                   (or (:snapping-points-cache db) {})
                   els))))

(m/=> snapping-points [:-> App [:maybe [:sequential Element]] [:vector Vec2]])
(defn snapping-points
  [db els]
  (if (active? db)
    (let [options (-> db :snap :options)
          cache (:snapping-points-cache db)]
      (into []
            (mapcat (fn [el]
                      (let [[cached-points cached-el cached-options]
                            (get cache (:id el))]
                        (if (and cached-points
                                 (= cached-el el)
                                 (= cached-options options))
                          cached-points
                          (utils.element/acc-snapping-points
                           el
                           (element.handlers/transform db (:id el))
                           options))))
                    els)))
    []))

(m/=> grid-axis-values [:-> App [:enum :x :y] [:sequential [:tuple number?]]])
(defn grid-axis-values
  [db axis]
  (map (fn [value]
         (with-meta [value] {:label [::grid-line "grid line"]}))
       (ruler.handlers/axis-steps db axis)))

(m/=> guide-axis-values [:-> App [:enum :x :y] [:sequential [:tuple number?]]])
(defn guide-axis-values
  [db axis]
  (let [horizontal? (= axis :y)
        orientation (if horizontal? "horizontal" "vertical")
        coord (if horizontal? :y :x)
        ids (vec (element.handlers/non-selected-ids db))]
    (into []
          (keep (fn [el]
                  (let [value (js/parseFloat (-> el :attrs coord))]
                    (when-not (js/isNaN value)
                      (with-meta [value] {:label [::guide "guide"]})))))
          (->> (element.handlers/entities db ids)
               (filter #(and (= (:tag %) :guide)
                             (= (-> % :attrs :orientation) orientation)))))))

(m/=> axis-values [:->
                   App [:vector Vec2] [:enum :x :y]
                   [:sequential [:tuple number?]]])
(defn axis-values
  [db points axis]
  (let [index (case axis :x 0 :y 1)
        options (-> db :snap :options)
        attrs (get-in db [:documents (:active-document db) :attrs])]
    (into (map (fn [point]
                 (with-meta [(nth point index)]
                   (assoc (meta point) :point point)))
               points)
          (cond-> []
            (and (contains? options :grid) (:grid attrs))
            (into (grid-axis-values db axis))

            (and (contains? options :guides) (:guides attrs))
            (into (guide-axis-values db axis))))))

(m/=> update-viewport-tree [:-> App App])
(defn update-viewport-tree
  [db]
  (if-not (active? db)
    db
    (let [[x y width height] (frame.handlers/viewbox db)
          boundaries [[x (+ x width)] [y (+ y height)]]
          points (kdtree/interval-search (:kdtree db) boundaries)]
      (assoc db
             :viewbox-kdtree (kdtree/build-tree points)
             :x-tree (kdtree/build-tree (axis-values db points :x))
             :y-tree (kdtree/build-tree (axis-values db points :y))))))

(m/=> rebuild-tree [:-> App App])
(defn rebuild-tree
  [db]
  (if (active? db)
    (let [elements (tool.hierarchy/snapping-elements db)
          db (update-snapping-points-cache
              (assoc db :snapping-points-cache {})
              elements)]
      (-> (assoc db :kdtree (kdtree/build-tree (snapping-points db elements)))
          (update-viewport-tree)))
    (dissoc db
            :kdtree :viewbox-kdtree :snapping-points-cache
            :x-tree :y-tree :nearest-x :nearest-y)))

(m/=> update-tree [:-> App ifn? [:vector Vec2] App])
(defn update-tree
  [db f points]
  (if (:kdtree db)
    (if (empty? points)
      db
      (-> (reduce #(update %1 :kdtree f %2) db points)
          (update-viewport-tree)))
    (rebuild-tree db)))

(m/=> insert-to-tree [:-> App [:maybe [:set ElementId]] App])
(defn insert-to-tree
  [db element-ids]
  (let [elements (element.handlers/entities db (vec element-ids))
        points (snapping-points db elements)]
    (update-tree db kdtree/insert points)))

(m/=> delete-from-tree [:-> App [:maybe [:set ElementId]] App])
(defn delete-from-tree
  [db element-ids]
  (let [elements (element.handlers/entities db (vec element-ids))
        points (snapping-points db elements)]
    (update-tree db kdtree/delete points)))

(m/=> nearest-delta [:-> App Vec2])
(defn nearest-delta
  [db]
  (if-let [{:keys [point base-point]} (:nearest-neighbor db)]
    (utils.math/v-sub point base-point)
    (let [dx (or (:delta (:nearest-x db)) 0)
          dy (or (:delta (:nearest-y db)) 0)]
      [dx dy])))

(m/=> snap-with [:-> App ifn? [:* any?] App])
(defn snap-with
  [db f & more]
  (if (active? db)
    (let [db (update-nearest-neighbors db)
          snapped? (or (:nearest-neighbor db)
                       (:nearest-x db)
                       (:nearest-y db))]
      (if snapped?
        (apply f db (nearest-delta db) more)
        db))
    db))
