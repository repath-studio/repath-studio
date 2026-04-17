(ns renderer.tool.impl.misc.guide
  (:require
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(tool.hierarchy/derive-tool :guide ::tool.hierarchy/tool)

(defn cursor
  [orientation]
  (if (= orientation :horizontal)
    "ns-resize"
    "ew-resize"))

(defmethod tool.hierarchy/on-activate :guide
  [db & {:as props}]
  (let [{:keys [orientation]} props]
    (-> db
        (tool.handlers/set-cursor (cursor orientation))
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :guide
                               :attrs {:orientation (name orientation)}}))))

(defmethod tool.hierarchy/on-pointer-move :guide
  [db _e]
  (let [[x y] (tool.handlers/snapped-position db)]
    (-> db
        (element.handlers/update-selected #(assoc-in % [:attrs :x] x))
        (element.handlers/update-selected #(assoc-in % [:attrs :y] y)))))

(defmethod tool.hierarchy/on-pointer-up :guide
  [db e]
  (-> db
      (history.handlers/finalize (:timestamp e) [::create-guide "Create guide"])
      (-> (tool.handlers/activate (:cached-tool db))
          (tool.handlers/set-state (:cached-state db))
          (dissoc :cached-tool :cached-state))))

(defmethod tool.hierarchy/snapping-points :guide
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label [::guide-position "guide position"]})])

(defmethod tool.hierarchy/snapping-elements :guide
  [db]
  (element.handlers/visible db))
