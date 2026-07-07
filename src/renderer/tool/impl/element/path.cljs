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
   [renderer.db :refer [Vec2]]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.input.db :refer [PointerEvent]]
   [renderer.input.handlers :as input.handlers]
   [renderer.input.subs :as-alias input.subs]
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

(m/=> adjusted-pointer-position [:-> App PointerEvent Vec2])
(defn adjusted-pointer-position
  [db e]
  (cond->> (tool.handlers/snapped-position db)
    :always
    (element.handlers/adjusted-point db)

    (input.handlers/snap-to-angle? db e)
    (input.handlers/snap-angle (->> (element.handlers/selected db)
                                    first :attrs :d
                                    (utils.path/string->segments)
                                    (take-last 2)
                                    (apply utils.path/abs-endpoint)
                                    (mapv utils.length/unit->px)))))

(m/=> adjusted-pointer-offset [:-> App Vec2])
(defn adjusted-pointer-offset
  [db]
  (->> (tool.handlers/snapped-offset db)
       (element.handlers/adjusted-point db)))

(m/=> update-path [:-> App fn? [:* any?] App])
(defn update-path
  [db f & more]
  (apply element.handlers/update-selected db update-in [:attrs :d] f more))

(m/=> add-to-path [:-> string? string? [:* number?] string?])
(defn add-to-path
  [d command & coords]
  (->> (mapv utils.length/->fixed coords)
       (into [d command])
       (string/join " ")))

(m/=> create-el [:-> App App])
(defn create-el
  [db]
  (let [path (->> (tool.handlers/snapped-offset db)
                  (mapv utils.length/->fixed)
                  (into ["M"])
                  (string/join " "))
        attrs (-> (document.handlers/attrs db)
                  (select-keys [:stroke :fill :stroke-width]))]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :path
                               :attrs (assoc attrs :d path)}))))

(defmethod tool.hierarchy/on-pointer-up [::path :idle]
  [db _e]
  (create-el db))

(defmethod tool.hierarchy/on-drag-start [::path :idle]
  [db _e]
  (create-el db))

(defmethod tool.hierarchy/on-pointer-move [::path :create]
  [db e]
  (let [[x y] (->> (adjusted-pointer-position db e)
                   (mapv utils.length/->fixed))]
    (update-path
     db
     #(let [segments (-> (utils.path/string->segments %)
                         (utils.path/drop-last-segment))
            last-segment (aget segments (dec (count segments)))
            out-cp (utils.path/outgoing-cp last-segment)
            flat? (and (= "S" (utils.path/segment->command last-segment))
                       (= (aget last-segment 1) (aget last-segment 3))
                       (= (aget last-segment 2) (aget last-segment 4)))]
        (-> segments
            (.concat #js [(if (and out-cp (not flat?))
                            #js ["S" x y x y]
                            #js ["L" x y])])
            (utils.path/segments->string))))))

(defmethod tool.hierarchy/on-pointer-up [::path :create]
  [db e]
  (let [[x y] (->> (adjusted-pointer-position db e)
                   (mapv utils.length/->fixed))]
    (update-path db (fn [d]
                      (let [segments (utils.path/string->segments d)
                            preview (aget segments (dec (count segments)))]
                        (-> (utils.path/drop-last-segment segments)
                            (.concat #js [preview #js ["L" x y]])
                            (utils.path/segments->string)))))))

(defmethod tool.hierarchy/on-drag [::path :create]
  [db _e]
  (let [anchor (adjusted-pointer-offset db)
        drag-pos (->> (tool.handlers/snapped-position db)
                      (element.handlers/adjusted-point db))
        [cp2-x cp2-y] (->> (matrix/sub (matrix/mul anchor 2) drag-pos)
                           (mapv utils.length/->fixed))]
    (update-path db #(let [segments (utils.path/string->segments %)]
                       (if (> (count segments) 1)
                         (let [[ax ay] (mapv utils.length/->fixed anchor)]
                           (-> (utils.path/drop-last-segment segments)
                               (.concat #js [#js ["S" cp2-x cp2-y ax ay]])
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
  (let [starting-point @(rf/subscribe [::input.subs/snapped-position])
        ending-point @(rf/subscribe [::input.subs/snapped-offset])
        drag? @(rf/subscribe [::input.subs/drag?])]
    (when drag?
      [utils.svg/arm starting-point ending-point])))

(rf/dispatch [::action.events/register-action
              {:id :tool/path
               :label [::label "Path"]
               :icon "bezier-curve"
               :event [::tool.events/activate ::path]
               :active [::tool.subs/active? ::path]
               :shortcuts [{:keyCode (utils.key/codes "P")}]}])
