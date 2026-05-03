(ns renderer.tool.impl.element.polyline
  "https://www.w3.org/TR/SVG/shapes.html#PolylineElement"
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.element.poly :as poly]
   [renderer.tool.subs :as-alias tool.subs]))

(hierarchy/derive! ::polyline ::tool.hierarchy/poly)

(defmethod tool.hierarchy/on-double-click [::polyline :create]
  [db e]
  (-> db
      (poly/drop-last-point)
      (history.handlers/finalize (:timestamp e)
                                 [::create-polyline "Create polyline"])
      (tool.handlers/deactivate)))

(defmethod tool.hierarchy/on-pointer-up [::polyline :idle]
  [db _e]
  (let [stroke (document.handlers/attr db :stroke)
        fill (document.handlers/attr db :fill)
        initial-point (tool.handlers/snapped-position db)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :polyline
                               :attrs {:points (string/join " " initial-point)
                                       :stroke stroke
                                       :fill fill}}))))

(rf/dispatch [::action.events/register-action
              {:id :tool/polyline
               :label [::label "Polyline"]
               :icon "polyline"
               :event [::tool.events/activate ::polyline]
               :active [::tool.subs/active? ::polyline]}])
