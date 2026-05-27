(ns renderer.tool.impl.base.transform.core
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.subs :as-alias element.subs]
   [renderer.hierarchy :as hierarchy]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.transform.clone]
   [renderer.tool.impl.base.transform.edit]
   [renderer.tool.impl.base.transform.idle]
   [renderer.tool.impl.base.transform.scale]
   [renderer.tool.impl.base.transform.select :as transform.select]
   [renderer.tool.impl.base.transform.translate]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.tool.views :as tool.views]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.length :as utils.length]
   [renderer.utils.svg :as utils.svg]))

(hierarchy/derive! ::transform ::tool.hierarchy/tool)

(defmethod tool.hierarchy/on-deactivate ::transform
  [db]
  (-> db
      (element.handlers/clear-ignored)
      (element.handlers/clear-hovered)
      (assoc :pivot-point [0 0])
      (assoc :anchor-offset (:anchor-point db))
      (transform.select/clear-select-box)))

(defn pivot-handle
  []
  (let [pivot-point @(rf/subscribe [::tool.subs/pivot-point])
        bbox @(rf/subscribe [::element.subs/bbox])
        anchor-offset @(rf/subscribe [::tool.subs/anchor-offset])
        state @(rf/subscribe [::tool.subs/state])
        handle-size @(rf/subscribe [::document.subs/handle-size])
        [x y] (when bbox
                (let [[min-x min-y] bbox
                      [w h] (utils.bounds/->dimensions bbox)
                      [fx fy] anchor-offset]
                  [(+ min-x (* fx w)) (+ min-y (* fy h))]))]
    [:g
     (when (and pivot-point (= state :scale))
       [utils.svg/times pivot-point])
     (when (and bbox (contains? #{:edit :idle} state))
       [:g
        [utils.svg/cross [x y] (* handle-size 1.5)]
        [tool.views/handle {:id :pivot-handle
                            :x x
                            :y y
                            :rounded true
                            :type :handle
                            :label [::pivot-point "pivot point"]
                            :action :edit}]])]))

(m/=> bounding-box [:-> Element boolean? any?])
(defn bounding-box
  [el dashed?]
  (some-> (:bbox el)
          (utils.svg/bounding-box dashed?)))

(defn area-label
  [bbox]
  (let [area @(rf/subscribe [::element.subs/area])
        zoom @(rf/subscribe [::document.subs/zoom])
        handle-size @(rf/subscribe [::document.subs/handle-size])]
    (when (pos? area)
      (let [[min-x min-y max-x] bbox
            x (+ min-x (/ (- max-x min-x) 2))
            y (- min-y (/ handle-size 2) (/ 15 zoom))
            text (str (utils.length/->fixed area 2 false) " px²")]
        [utils.svg/label text {:x x
                               :y y}]))))

(defn size-label
  [bbox]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        handle-size @(rf/subscribe [::document.subs/handle-size])
        [min-x _min-y max-x y2] bbox
        x (+ min-x (/ (- max-x min-x) 2))
        y (+ y2 (/ handle-size 2) (/ 15 zoom))
        [w h] (utils.bounds/->dimensions bbox)
        text (str (utils.length/->fixed w 2 false)
                  " x "
                  (utils.length/->fixed h 2 false))]
    [utils.svg/label text {:x x
                           :y y}]))

(defmethod tool.hierarchy/render ::transform
  []
  (let [state @(rf/subscribe [::tool.subs/state])
        selected-elements @(rf/subscribe [::element.subs/selected])
        bbox @(rf/subscribe [::element.subs/bbox])
        hovered-elements @(rf/subscribe [::element.subs/hovered])
        touch? @(rf/subscribe [::app.subs/supported-feature? :touch])]
    [:<>
     (into [:<>]
           (map #(bounding-box % false) selected-elements))

     (when (or (not touch?) (= state :select))
       (into [:<>]
             (map #(bounding-box % true) hovered-elements)))

     (when (seq bbox)
       [:<>
        [tool.views/selected-bbox bbox]
        (when (= state :idle)
          [tool.views/corner-handles bbox])])

     (when (and (= state :scale) (seq bbox))
       [:<>
        [area-label bbox]
        [size-label bbox]])

     [pivot-handle]

     [transform.select/render-select-box]]))

(rf/dispatch [::action.events/register-action
              {:id :tool/transform
               :label [::label "Transform"]
               :icon "pointer"
               :event [::tool.events/activate ::transform]
               :active [::tool.subs/active? ::transform]}])

(rf/reg-global-interceptor
 (rf/->interceptor
  :id ::clear-anchor
  :after (fn [context]
           (let [db (rf/get-effect context :db)]
             (if (:active-document db)
               (let [selected-ids (element.handlers/selected-ids db)
                     prev-selected-ids
                     (let [db (rf/get-coeffect context :db)]
                       (when (:active-document db)
                         (element.handlers/selected-ids db)))]
                 (cond-> context
                   (not= selected-ids prev-selected-ids)
                   (rf/assoc-effect :db (assoc db
                                               :anchor-point [0.5 0.5]
                                               :anchor-offset [0.5 0.5]))))
               context)))))
