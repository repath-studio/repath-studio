(ns renderer.tool.impl.element.path
  "Pen/bezier path drawing tool.
   Click to place anchor points; click and drag to pull out bezier handles."
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.app.db :refer [App]]
   [renderer.app.subs :as-alias app.subs]
   [renderer.db :refer [Vec2]]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.key :as utils.key]
   [renderer.utils.length :as utils.length]
   [renderer.utils.path :as utils.path]
   [renderer.utils.svg :as utils.svg]))

(hierarchy/derive! ::path ::tool.hierarchy/element)

(defmethod tool.hierarchy/help [::path :idle]
  []
  (i18n.views/t [::click-to-start "Click to add the first point."]))

(defmethod tool.hierarchy/help [::path :create]
  []
  [:<>
   [:div (i18n.views/t [::click-to-add
                        "Click to add a segment, or click and drag to add a
                         curve."])]
   [:div (i18n.views/t [::double-click-to-end
                        "Double or right click to finalize the path."])]])

(m/=> next-control-point [:-> [:vector any?] [:tuple string? string?]])
(defn next-control-point
  [segments]
  (let [segment (-> segments utils.path/drop-last-segment last)]
    (or (utils.path/outgoing-cp segment)
        (utils.path/segment-point segment :end-point))))

(m/=> adjusted-pointer-position [:-> App Vec2])
(defn adjusted-pointer-position
  [db]
  (->> (tool.handlers/snapped-position db)
       (element.handlers/adjusted-point db)))

(m/=> adjusted-pointer-position [:-> App Vec2])
(defn adjusted-pointer-offset
  [db]
  (->> (tool.handlers/snapped-offset db)
       (element.handlers/adjusted-point db)))

(m/=> update-path [:-> App App])
(defn update-path
  [db f & args]
  (apply element.handlers/update-selected db update-in [:attrs :d] f args))

(m/=> add-to-path [:-> string? string?])
(defn add-to-path
  [d command & coords]
  (->> (mapv utils.length/->fixed coords)
       (into [d command])
       (string/join " ")))

(m/=> create-el [:-> App App])
(defn create-el
  [db]
  (let [[x y] (->> (tool.handlers/snapped-offset db)
                   (mapv utils.length/->fixed))
        stroke (document.handlers/attr db :stroke)
        fill (document.handlers/attr db :fill)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :path
                               :attrs {:d (string/join " " ["M" x y])
                                       :stroke stroke
                                       :fill fill}}))))

(defmethod tool.hierarchy/on-pointer-up [::path :idle]
  [db _e]
  (create-el db))

(defmethod tool.hierarchy/on-drag-start [::path :idle]
  [db _e]
  (create-el db))

(defmethod tool.hierarchy/on-pointer-move [::path :create]
  [db _e]
  (let [[x y] (adjusted-pointer-position db)]
    (update-path
     db
     #(let [segments (-> (utils.path/string->segments %)
                         (utils.path/drop-last-segment))
            last-segment (last segments)
            out-cp (utils.path/outgoing-cp last-segment)]
        (->> (if out-cp
               ["C" (first out-cp) (second out-cp) x y x y]
               ["L" x y])
             (into segments)
             (utils.path/segments->string))))))

(defmethod tool.hierarchy/on-pointer-up [::path :create]
  [db _e]
  (let [[x y] (adjusted-pointer-position db)]
    (update-path db add-to-path "L" x y)))

(defmethod tool.hierarchy/on-drag [::path :create]
  [db _e]
  (let [anchor (adjusted-pointer-offset db)
        drag-pos (adjusted-pointer-position db)
        [cp2-x cp2-y] (-> (matrix/mul anchor 2)
                          (matrix/sub drag-pos))]
    (update-path db #(let [segments (utils.path/string->segments %)]
                       (if (> (count segments) 1)
                         (let [[ax ay] anchor
                               [cp1-x cp1-y] (next-control-point segments)]
                           (->> ["C" cp1-x cp1-y cp2-x cp2-y ax ay]
                                (into (utils.path/drop-last-segment segments))
                                (utils.path/segments->string)))
                         %)))))

(defmethod tool.hierarchy/on-drag-end [::path :create]
  [db e]
  (let [[x y] (adjusted-pointer-offset db)]
    (-> (update-path db add-to-path "L" x y)
        (tool.hierarchy/on-pointer-move db e))))

(defmethod tool.hierarchy/on-double-click [::path :create]
  [db e]
  (-> (update-path db (comp utils.path/segments->string
                            utils.path/drop-last-segment
                            utils.path/string->segments))
      (history.handlers/finalize (:timestamp e) [::create-path "Create path"])
      (tool.handlers/deactivate)))

(defmethod tool.hierarchy/on-context-menu [::path :create]
  [db e]
  (tool.hierarchy/on-double-click db e))

(defmethod tool.hierarchy/render ::path
  []
  (let [starting-point @(rf/subscribe [::tool.subs/snapped-position])
        ending-point @(rf/subscribe [::tool.subs/snapped-offset])
        drag? @(rf/subscribe [::app.subs/drag?])]
    (when drag?
      [utils.svg/arm starting-point ending-point])))

(rf/dispatch [::action.events/register-action
              {:id :tool/path
               :label [::label "Path"]
               :icon "bezier-curve"
               :event [::tool.events/activate ::path]
               :active [::tool.subs/active? ::path]
               :shortcuts [{:keyCode (utils.key/codes "P")}]}])
