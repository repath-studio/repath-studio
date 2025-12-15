(ns renderer.tool.impl.element.poly
  "An abstraction for polygons and polylines that have similar behavior."
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.attribute :as utils.attribute]))

(tool.hierarchy/derive-tool ::tool.hierarchy/poly ::tool.hierarchy/element)

(defmethod tool.hierarchy/help [::tool.hierarchy/poly :idle]
  []
  [:<>
   [:div (i18n.views/t [::add-points "Click to add more points."])]
   [:div (i18n.views/t [::finalize-shape
                        "Double click to finalize the shape."])]])

(defn create-el
  [db initial-point]
  (let [stroke (document.handlers/attr db :stroke)
        fill (document.handlers/attr db :fill)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag (:tool db)
                               :attrs {:points (string/join " " initial-point)
                                       :stroke stroke
                                       :fill fill}}))))

(defn add-point
  [db point]
  (if (= (:state db) :create)
    (element.handlers/update-selected db
                                      update-in [:attrs :points]
                                      str " " (string/join " " point))
    (create-el db point)))

(defn drop-last-point
  [db]
  (element.handlers/update-selected db
                                    update-in [:attrs :points]
                                    #(->> (utils.attribute/points->vec %)
                                          (drop-last)
                                          (flatten)
                                          (string/join " "))))

(defn adjusted-point
  [db point]
  (matrix/sub point (element.handlers/parent-offset db)))

(defmethod tool.hierarchy/on-pointer-up ::tool.hierarchy/poly
  [db _e]
  (let [point (tool.handlers/snapped-position db)
        point (cond->> point
                (= (:state db) :create)
                (adjusted-point db))]
    (add-point db point)))

(defmethod tool.hierarchy/on-drag-end ::tool.hierarchy/poly
  [db e]
  (tool.hierarchy/on-pointer-up db e))

(defmethod tool.hierarchy/on-pointer-move ::tool.hierarchy/poly
  [db _e]
  (let [point (tool.handlers/snapped-position db)
        point (adjusted-point db point)]
    (cond-> db
      (= (:state db) :create)
      (element.handlers/update-selected
       update-in [:attrs :points]
       #(let [point-vector (utils.attribute/points->vec %)
              point-vector (cond-> point-vector
                             (second point-vector)
                             (drop-last))]
          (->> (concat point-vector point)
               (flatten)
               (string/join " ")))))))
