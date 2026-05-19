(ns renderer.tool.impl.element.poly
  "An abstraction for polygons and polylines that have similar behavior."
  (:require
   [clojure.string :as string]
   [renderer.element.handlers :as element.handlers]
   [renderer.hierarchy :as hierarchy]
   [renderer.i18n.views :as i18n.views]
   [renderer.input.handlers :as input.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.length :as utils.length]))

(hierarchy/derive! ::tool.hierarchy/poly ::tool.hierarchy/element)

(defmethod tool.hierarchy/help [::tool.hierarchy/poly :idle]
  []
  (i18n.views/t [::click-to-start "Click to start drawing."]))

(defmethod tool.hierarchy/help [::tool.hierarchy/poly :create]
  []
  [:<>
   [:div (i18n.views/t [::add-points "Click to add more points."])]
   [:div (i18n.views/t [::finalize-shape
                        "Right click or double click to finalize the
                         shape."])]])

(defn update-points
  [db f]
  (element.handlers/update-selected db update-in [:attrs :points] f))

(defn drop-last-point
  [db]
  (update-points db #(->> (utils.attribute/points->vec %)
                          (drop-last)
                          (flatten)
                          (string/join " "))))

(defn adjusted-pointer-position
  [db e]
  (cond->> (tool.handlers/snapped-position db)
    :always
    (element.handlers/adjusted-point db)

    (input.handlers/snap-to-angle? db e)
    (input.handlers/snap-angle (->> (element.handlers/selected db)
                                    first :attrs :points
                                    (utils.attribute/points->vec)
                                    pop peek
                                    (mapv utils.length/unit->px)))

    :always
    (mapv utils.length/->fixed)))

(defmethod tool.hierarchy/on-pointer-up [::tool.hierarchy/poly :create]
  [db e]
  (update-points db #(->> (adjusted-pointer-position db e)
                          (into [%])
                          (string/join " "))))

(defmethod tool.hierarchy/on-drag-end [::tool.hierarchy/poly :create]
  [db e]
  (tool.hierarchy/on-pointer-up db e))

(defmethod tool.hierarchy/on-context-menu [::tool.hierarchy/poly :create]
  [db e]
  (tool.hierarchy/on-double-click db e))

(defmethod tool.hierarchy/on-pointer-move [::tool.hierarchy/poly :create]
  [db e]
  (let [point (adjusted-pointer-position db e)]
    (cond-> db
      (= (:state db) :create)
      (update-points #(let [point-vector (utils.attribute/points->vec %)
                            point-vector (cond-> point-vector
                                           (second point-vector)
                                           (drop-last))]
                        (->> (concat point-vector point)
                             (flatten)
                             (string/join " ")))))))

(defmethod tool.hierarchy/on-drag-start [::tool.hierarchy/poly :idle]
  [db e]
  (tool.hierarchy/on-pointer-up db e))

(defmethod tool.hierarchy/on-drag [::tool.hierarchy/poly :create]
  [db e]
  (tool.hierarchy/on-pointer-move db e))
