(ns renderer.tool.impl.element.circle
  "https://www.w3.org/TR/SVG/shapes.html#CircleElement"
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.key :as utils.key]
   [renderer.utils.math :as utils.math]))

(hierarchy/derive! ::circle ::tool.hierarchy/element)

(defn create-el
  [db]
  (let [parent-id (:id (element.handlers/hovered-svg db))
        to-local (partial element.handlers/container-local-point db parent-id)
        offset (to-local (tool.handlers/snapped-offset db))
        position (to-local (tool.handlers/snapped-position db))
        radius (utils.math/distance position offset)
        [cx cy] offset
        attrs (-> (document.handlers/attrs db)
                  (select-keys [:stroke :fill :stroke-width]))]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :circle
                               :parent parent-id
                               :attrs (merge attrs {:cx cx
                                                    :cy cy
                                                    :r radius})}))))

(defn update-el
  [db]
  (let [{:keys [id attrs]} (first (element.handlers/selected db))
        position (->> (tool.handlers/snapped-position db)
                      (element.handlers/local-point db id))
        {:keys [cx cy]} attrs
        radius (-> position
                   (utils.math/distance [cx cy])
                   (utils.attribute/->fixed))]
    (element.handlers/update-selected db #(assoc-in % [:attrs :r] radius))))

(defn finalize
  [db e]
  (-> db
      (history.handlers/finalize (:timestamp e)
                                 [::create-circle "Create circle"])
      (tool.handlers/deactivate)))

(defmethod tool.hierarchy/on-drag-start [::circle :idle]
  [db _e]
  (create-el db))

(defmethod tool.hierarchy/on-pointer-up [::circle :idle]
  [db _e]
  (create-el db))

(defmethod tool.hierarchy/on-drag [::circle :create]
  [db _e]
  (update-el db))

(defmethod tool.hierarchy/on-pointer-down [::circle :create]
  [db _e]
  (update-el db))

(defmethod tool.hierarchy/on-pointer-move [::circle :create]
  [db _e]
  (update-el db))

(defmethod tool.hierarchy/on-drag-end [::circle :create]
  [db e]
  (finalize db e))

(defmethod tool.hierarchy/on-pointer-up [::circle :create]
  [db e]
  (finalize db e))

(defmethod tool.hierarchy/snapping-points [::circle :create]
  [db]
  [(with-meta
     (:local-pointer-pos db)
     {:label (if (= (:state db) :create)
               [::circle-radius "circle radius"]
               [::circle-center "circle center"])})])

(rf/dispatch [::action.events/register-action
              {:id :tool/circle
               :label [::label "Circle"]
               :icon "circle-tool"
               :event [::tool.events/activate ::circle]
               :active [::tool.subs/active? ::circle]
               :shortcuts {"All" [{:keyCode (utils.key/codes "C")}]}}])
