(ns renderer.tool.impl.element.ellipse
  "https://www.w3.org/TR/SVG/shapes.html#EllipseElement"
  (:require
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.length :as utils.length]
   [renderer.views :as views]))

(tool.hierarchy/derive-tool :ellipse ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :ellipse
  []
  {:icon "ellipse-tool"
   :label [::label "Ellipse"]})

(defmethod tool.hierarchy/help [:ellipse :create]
  []
  (i18n.views/t [::help [:div "Hold %1 to lock proportions."]]
                [[views/kbd "Ctrl"]]))

(defn attributes
  [db lock-ratio]
  (let [[offset-x offset-y] (tool.handlers/snapped-offset db)
        [x y] (tool.handlers/snapped-position db)
        rx (abs (- x offset-x))
        ry (abs (- y offset-y))]
    {:rx (utils.length/->fixed (cond-> rx lock-ratio (min ry)))
     :ry (utils.length/->fixed (cond-> ry lock-ratio (min rx)))}))

(defmethod tool.hierarchy/on-drag-start :ellipse
  [db e]
  (let [[x y] (tool.handlers/snapped-position db)
        fill (document.handlers/attr db :fill)
        stroke (document.handlers/attr db :stroke)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :ellipse
                               :attrs (merge (attributes db (:ctrl-key e))
                                             {:cx x
                                              :cy y
                                              :fill fill
                                              :stroke stroke})}))))

(defmethod tool.hierarchy/on-drag :ellipse
  [db e]
  (let [attrs (attributes db (:ctrl-key e))
        assoc-attr (fn [el [k v]] (assoc-in el [:attrs k] (str v)))]
    (element.handlers/update-selected db #(reduce assoc-attr % attrs))))

(defmethod tool.hierarchy/on-drag-end :ellipse
  [db _e]
  (-> db
      (history.handlers/finalize [::create-ellipse "Create ellipse"])
      (tool.handlers/activate :transform)))
