(ns renderer.tool.impl.element.line
  "https://www.w3.org/TR/SVG/shapes.html#LineElement"
  (:require
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.input.handlers :as input.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.key :as utils.key]
   [renderer.utils.length :as utils.length]))

(hierarchy/derive! ::line ::tool.hierarchy/element)

(defn create-el
  [db]
  (let [[offset-x offset-y] (tool.handlers/snapped-offset db)
        [x y] (tool.handlers/snapped-position db)
        attrs (-> (document.handlers/attrs db)
                  (select-keys [:stroke :stroke-width]))]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :line
                               :attrs (merge attrs {:x1 offset-x
                                                    :y1 offset-y
                                                    :x2 x
                                                    :y2 y})}))))

(defn update-el
  [db e]
  (let [pointer-pos (tool.handlers/snapped-position db)
        end-pos (matrix/sub pointer-pos (element.handlers/parent-offset db))
        {:keys [x1 y1]} (->> db element.handlers/selected first :attrs)
        start-pos (mapv utils.length/unit->px [x1 y1])
        end-pos (cond->> end-pos
                  (input.handlers/snap-to-angle? db e)
                  (input.handlers/snap-angle start-pos))
        [x2 y2] (mapv utils.length/->fixed end-pos)]
    (element.handlers/update-selected db #(-> %
                                              (assoc-in [:attrs :x2] x2)
                                              (assoc-in [:attrs :y2] y2)))))

(defn finalize
  [db e]
  (-> db
      (history.handlers/finalize (:timestamp e) [::create-line "Create line"])
      (tool.handlers/deactivate)))

(defmethod tool.hierarchy/on-drag-start [::line :idle]
  [db _e]
  (create-el db))

(defmethod tool.hierarchy/on-pointer-up [::line :idle]
  [db _e]
  (create-el db))

(defmethod tool.hierarchy/on-drag [::line :create]
  [db e]
  (update-el db e))

(defmethod tool.hierarchy/on-pointer-down [::line :create]
  [db e]
  (update-el db e))

(defmethod tool.hierarchy/on-pointer-move [::line :create]
  [db e]
  (update-el db e))

(defmethod tool.hierarchy/on-drag-end [::line :create]
  [db e]
  (finalize db e))

(defmethod tool.hierarchy/on-pointer-up [::line :create]
  [db e]
  (finalize db e))

(rf/dispatch [::action.events/register-action
              {:id :tool/line
               :label [::label "Line"]
               :icon "line-tool"
               :event [::tool.events/activate ::line]
               :active [::tool.subs/active? ::line]
               :shortcuts [{:keyCode (utils.key/codes "L")}]}])
