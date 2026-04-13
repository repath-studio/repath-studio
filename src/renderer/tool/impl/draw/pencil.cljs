(ns renderer.tool.impl.draw.pencil
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.element :as utils.element]
   [renderer.utils.path :as utils.path]))

(tool.hierarchy/derive-tool :pencil ::tool.hierarchy/draw)

(defmethod tool.hierarchy/on-drag-start :pencil
  [db _e]
  (let [stroke (document.handlers/attr db :stroke)
        point-1 (string/join " " (:adjusted-pointer-offset db))
        point-2 (string/join " " (:adjusted-pointer-pos db))]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :polyline
                               :attrs {:points (str point-1 " " point-2)
                                       :stroke stroke
                                       :fill "transparent"}}))))

(defmethod tool.hierarchy/on-drag :pencil
  [db _e]
  (let [[min-x min-y] (element.handlers/parent-offset db)
        point (matrix/sub (:adjusted-pointer-pos db) [min-x min-y])
        point (string/join " " point)]
    (element.handlers/update-selected db
                                      update-in [:attrs :points]
                                      str " " point)))

(defmethod tool.hierarchy/on-drag-end :pencil
  [db e]
  (let [path (-> (first (element.handlers/selected db))
                 (utils.element/->path)
                 (update-in [:attrs :d] utils.path/manipulate :smooth)
                 (update-in [:attrs :d] utils.path/manipulate :simplify))]
    (-> db
        (element.handlers/swap path)
        (history.handlers/finalize (:timestamp e) [::draw-line "Draw line"])
        (tool.handlers/activate :transform))))

(rf/dispatch [::action.events/register-action
              {:id :tool/pencil
               :label [::label "Pen"]
               :icon "pencil"
               :event [::tool.events/activate :pencil]
               :active [::tool.subs/active? :pencil]}])
