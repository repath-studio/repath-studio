(ns renderer.tool.impl.element.line
  "https://www.w3.org/TR/SVG/shapes.html#LineElement"
  (:require
   [clojure.core.matrix :as matrix]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.length :as utils.length]))

(tool.hierarchy/derive-tool :line ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :line
  []
  {:icon "line-tool"
   :label [::label "Line"]})

(defmethod tool.hierarchy/on-drag-start :line
  [db _e]
  (let [[offset-x offset-y] (tool.handlers/snapped-offset db)
        [x y] (tool.handlers/snapped-position db)
        stroke (document.handlers/attr db :stroke)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :line
                               :attrs {:x1 offset-x
                                       :y1 offset-y
                                       :x2 x
                                       :y2 y
                                       :stroke stroke}}))))

(defmethod tool.hierarchy/on-drag :line
  [db _e]
  (let [position (tool.handlers/snapped-position db)
        [min-x min-y] (element.handlers/parent-offset db)
        [x y] (matrix/sub position [min-x min-y])
        x (utils.length/->fixed x)
        y (utils.length/->fixed y)]
    (element.handlers/update-selected db #(-> %
                                              (assoc-in [:attrs :x2] x)
                                              (assoc-in [:attrs :y2] y)))))

(defmethod tool.hierarchy/on-drag-end :line
  [db _e]
  (-> db
      (history.handlers/finalize [::create-line "Create line"])
      (tool.handlers/activate :transform)))
