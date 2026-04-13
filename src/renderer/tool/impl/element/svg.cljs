(ns renderer.tool.impl.element.svg
  "https://www.w3.org/TR/SVG/struct.html#SVGElement"
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.key :as utils.key]
   [renderer.utils.length :as utils.length]
   [renderer.views :as views]))

(tool.hierarchy/derive-tool :svg ::tool.hierarchy/element)

(defmethod tool.hierarchy/help [:svg :create]
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

(defmethod tool.hierarchy/on-drag-start :svg
  [db e]
  (-> db
      (tool.handlers/set-state :create)
      (element.handlers/add {:tag :svg
                             :type :element
                             :attrs (attributes db (:ctrl-key e))})))

(defmethod tool.hierarchy/on-drag :svg
  [db e]
  (let [lock-ratio (or (:ctrl-key e) (tool.handlers/multi-touch? db))
        attrs (attributes db lock-ratio)
        assoc-attr (fn [el [k v]] (assoc-in el [:attrs k] (str v)))]
    (element.handlers/update-selected db #(reduce assoc-attr % attrs))))

(defmethod tool.hierarchy/on-drag-end :svg
  [db e]
  (-> db
      (history.handlers/finalize (:timestamp e) [::create-svg "Create SVG"])
      (tool.handlers/activate :transform)))

(rf/dispatch [::action.events/register-action
              {:id :tool/svg
               :label [::label "Svg"]
               :icon "svg"
               :event [::tool.events/activate :svg]
               :active [::tool.subs/active? :svg]
               :shortcuts [{:keyCode (utils.key/codes "S")}]}])
