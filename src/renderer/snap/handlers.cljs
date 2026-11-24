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
   [renderer.snap.db :refer [NearestNeighbor SnapOption SnapOptions]]
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

(m/=> update-nearest-neighbors [:-> App App])
(defn update-nearest-neighbors
  [db]
  (if-not (active? db)
    db
    (let [zoom (get-in db [:documents (:active-document db) :zoom])
          threshold (Math/pow (/ config/snap-threshold zoom) 2)
          nneighbors (nearest-neighbors db)
          nneighbors (filter #(< (:dist-squared %) threshold) nneighbors)]
      (assoc db
             :nearest-neighbors nneighbors
             :nearest-neighbor (apply min-key :dist-squared nneighbors)))))

(m/=> update-viewport-tree [:-> App App])
(defn update-viewport-tree
  [db]
  (if-not (active? db)
    db
    (let [[x y width height] (frame.handlers/viewbox db)
          boundaries [[x (+ x width)] [y (+ y height)]]]
      (assoc db :viewbox-kdtree (-> (:kdtree db)
                                    (kdtree/interval-search boundaries)
                                    (kdtree/build-tree))))))

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

(m/=> rebuild-tree [:-> App App])
(defn rebuild-tree
  [db]
  (if (active? db)
    (let [elements (tool.hierarchy/snapping-elements db)
          db (update-snapping-points-cache
              (assoc db :snapping-points-cache {})
              elements)
          points (snapping-points db elements)
          points (cond-> points
                   (contains? (-> db :snap :options) :grid)
                   (into (ruler.handlers/steps-intersections db)))]
      (-> (assoc db :kdtree (kdtree/build-tree points))
          (update-viewport-tree)))
    (dissoc db :kdtree :viewbox-kdtree :snapping-points-cache)))

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
  (or (when-let [{:keys [point base-point]} (:nearest-neighbor db)]
        (utils.math/v-sub point base-point))
      [0 0]))

(m/=> snap-with [:-> App ifn? [:* any?] App])
(defn snap-with
  [db f & more]
  (if (active? db)
    (let [db (update-nearest-neighbors db)]
      (if (:nearest-neighbor db)
        (apply f db (nearest-delta db) more)
        db))
    db))
