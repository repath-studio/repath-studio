(ns renderer.tool.impl.element.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement"
  (:require
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.length :as utils.length]
   [renderer.views :as views]))

(tool.hierarchy/derive-tool :rect ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :rect
  []
  {:icon "rectangle-tool"
   :label [::label "Rectangle"]})

(defmethod tool.hierarchy/help [:rect :create]
  []
  (i18n.views/t [::help [:div "Hold %1 to lock proportions."]]
                [[views/kbd "Ctrl"]]))

(defn attributes
  [db lock-ratio]
  (let [[offset-x offset-y] (tool.handlers/snapped-offset db)
        [x y] (tool.handlers/snapped-position db)
        width (abs (- x offset-x))
        height (abs (- y offset-y))
        width (cond-> width lock-ratio (min height))
        height (cond-> height lock-ratio (min width))]
    {:x (utils.length/->fixed (cond-> offset-x (< x offset-x) (- width)))
     :y (utils.length/->fixed (cond-> offset-y (< y offset-y) (- height)))
     :width (utils.length/->fixed width)
     :height (utils.length/->fixed height)}))

(defmethod tool.hierarchy/on-drag-start :rect
  [db e]
  (let [fill (document.handlers/attr db :fill)
        stroke (document.handlers/attr db :stroke)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :rect
                               :attrs (merge (attributes db (:ctrl-key e))
                                             {:fill fill
                                              :stroke stroke})}))))

(defmethod tool.hierarchy/on-drag :rect
  [db e]
  (let [lock-ratio (or (:ctrl-key e) (tool.handlers/multi-touch? db))
        attrs (attributes db lock-ratio)
        assoc-attr (fn [el [k v]] (assoc-in el [:attrs k] (str v)))
        [min-x min-y] (element.handlers/parent-offset db)]
    (-> db
        (element.handlers/update-selected #(reduce assoc-attr % attrs))
        (element.handlers/translate [(- min-x) (- min-y)]))))

(defmethod tool.hierarchy/on-drag-end :rect
  [db _e]
  (-> db
      (history.handlers/finalize [::create-rectangle "Create rectangle"])
      (tool.handlers/activate :transform)))
