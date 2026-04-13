(ns renderer.tool.impl.element.circle
  "https://www.w3.org/TR/SVG/shapes.html#CircleElement"
  (:require
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.key :as utils.key]
   [renderer.utils.length :as utils.length]))

(tool.hierarchy/derive-tool :circle ::tool.hierarchy/element)

(defmethod tool.hierarchy/on-drag-start :circle
  [db _e]
  (let [offset (tool.handlers/snapped-offset db)
        position (tool.handlers/snapped-position db)
        radius (matrix/distance position offset)
        [cx cy] offset
        fill (document.handlers/attr db :fill)
        stroke (document.handlers/attr db :stroke)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :circle
                               :attrs {:cx cx
                                       :cy cy
                                       :fill fill
                                       :stroke stroke
                                       :r radius}}))))

(defmethod tool.hierarchy/on-drag :circle
  [db _e]
  (let [offset (tool.handlers/snapped-offset db)
        position (tool.handlers/snapped-position db)
        radius (utils.length/->fixed (matrix/distance position offset))]
    (element.handlers/update-selected db #(assoc-in % [:attrs :r] radius))))

(defmethod tool.hierarchy/on-drag-end :circle
  [db e]
  (-> db
      (history.handlers/finalize (:timestamp e)
                                 [::create-circle "Create circle"])
      (tool.handlers/activate :transform)))

(defmethod tool.hierarchy/snapping-points :circle
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label (if (= (:state db) :create)
               [::circle-radius "circle radius"]
               [::circle-center "circle center"])})])

(rf/dispatch [::action.events/register-action
              {:id :tool/circle
               :label [::label "Circle"]
               :icon "circle-tool"
               :event [::tool.events/activate :circle]
               :active [::tool.subs/active? :circle]
               :shortcuts [{:keyCode (utils.key/codes "C")}]}])
