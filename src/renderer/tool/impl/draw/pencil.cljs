(ns renderer.tool.impl.draw.pencil
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.document.events :as-alias document.events]
   [renderer.document.handlers :as document.handlers]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.handlers :as element.handlers]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.path :as utils.path]
   [renderer.views :as views]))

(hierarchy/derive! ::pencil ::tool.hierarchy/draw)

(rf/dispatch [::action.events/register-action
              {:id :pencil/toggle-smooth
               :label [::smooth "Smooth"]
               :icon "smooth"
               :event [::document.events/toggle-attr ::smooth]
               :active [::document.subs/attr ::smooth]
               :enabled [::tool.subs/active? ::pencil]}])

(rf/dispatch [::action.events/register-action-group
              {:id :pencil/options
               :icon "toggles"
               :label [::pen-options "Pencil options"]
               :enabled [::tool.subs/active? ::pencil]
               :actions [:pencil/toggle-smooth]}])

(defmethod tool.hierarchy/tool-options ::pencil
  []
  [views/action-button-group :pencil/options])

(defmethod tool.hierarchy/on-drag-start [::pencil :idle]
  [db _e]
  (let [stroke (document.handlers/attr db :stroke)
        parent-id (:id (element.handlers/hovered-svg db))
        to-local (partial element.handlers/container-local-point db parent-id)
        point-1 (string/join " " (to-local (:local-pointer-offset db)))
        point-2 (string/join " " (to-local (:local-pointer-pos db)))]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :path
                               :parent parent-id
                               :attrs {:d (str "M " point-1 " Q " point-2)
                                       :stroke stroke
                                       :fill "transparent"}}))))

(defmethod tool.hierarchy/on-drag [::pencil :create]
  [db _e]
  (let [{:keys [local-pointer-pos]} db
        id (-> db element.handlers/selected first :id)
        point (element.handlers/local-point db id local-pointer-pos)
        point (string/join " " point)]
    (element.handlers/update-selected db
                                      update-in [:attrs :d]
                                      str " " point)))

(defmethod tool.hierarchy/on-drag-end [::pencil :create]
  [db e]
  (let [path (cond-> (first (element.handlers/selected db))
               (document.handlers/attr db ::smooth)
               (update-in [:attrs :d] utils.path/manipulate :simplify))]
    (-> db
        (element.handlers/swap path)
        (history.handlers/finalize (:timestamp e) [::draw-line "Draw line"])
        (tool.handlers/deactivate))))

(rf/dispatch [::action.events/register-action
              {:id :tool/pencil
               :label [::label "Pencil"]
               :icon "pencil"
               :event [::tool.events/activate ::pencil]
               :active [::tool.subs/active? ::pencil]}])
