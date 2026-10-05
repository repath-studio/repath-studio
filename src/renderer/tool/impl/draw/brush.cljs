(ns renderer.tool.impl.draw.brush
  "https://github.com/steveruizok/perfect-freehand"
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.action.events :as-alias action.events]
   [renderer.app.handlers :as app.handlers]
   [renderer.document.events :as-alias document.events]
   [renderer.document.handlers :as document.handlers]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.element :as utils.element]
   [renderer.utils.key :as utils.key]
   [renderer.views :as views]))

(hierarchy/derive! ::brush ::tool.hierarchy/draw)

(defonce brush (reagent/atom nil))

(def min-size 1)
(def max-size 100)
(def default-size 16)

(rf/reg-fx
 ::set-brush
 (fn [value]
   (reset! brush value)))

(rf/reg-fx
 ::set-brush-size
 (fn [value]
   (reset! brush {:type :element
                  :tag :circle
                  :attrs (assoc (:attrs @brush)
                                :r (str (/ value 2)))})))

(defn hovered-scale
  [db]
  (-> (element.handlers/hovered-svg db)
      (utils.element/transform)
      first))

(defn update-brush-size
  [db]
  (let [brush-size (document.handlers/attr db ::size)
        scale (hovered-scale db)]
    (app.handlers/enqueue-fx db [::set-brush-size (* brush-size scale)])))

(defmethod tool.hierarchy/help [::brush :idle]
  []
  (i18n.views/t [::help-idle [:div "Click and drag to brush.
                                    Hold %1 to adjust the brush size."]]
                [[views/kbd "⇧"]]))

(defmethod tool.hierarchy/tool-options ::brush
  []
  (let [size (or @(rf/subscribe [::document.subs/attr ::size]) default-size)]
    [:div.flex.items-center.gap-2.p-1
     [:span size]
     [views/slider
      {:min min-size
       :max max-size
       :step 1
       :title (i18n.views/t [::brush-size "Brush size"])
       :value [size]
       :class "w-32"
       :on-value-change (fn [[v]]
                          (rf/dispatch [::document.events/set-attr
                                        ::size v]))}]]))

(defmethod tool.hierarchy/on-pointer-move [::brush :idle]
  [db _e]
  (let [size (or (document.handlers/attr db ::size) default-size)
        [x y] (:adjusted-pointer-pos db)
        fill (document.handlers/attr db :fill)
        radius (str (/ (* size (hovered-scale db)) 2))]
    (app.handlers/enqueue-fx db [::set-brush {:type :element
                                              :tag :circle
                                              :attrs {:cx (str x)
                                                      :cy (str y)
                                                      :r radius
                                                      :fill fill}}])))

(defmethod tool.hierarchy/on-drag-start [::brush :idle]
  [db e]
  (let [brush-size (or (document.handlers/attr db ::size) default-size)
        parent-id (:id (element.handlers/hovered-svg db))
        to-local (partial element.handlers/container-local-point db parent-id)
        point (string/join " " (conj (to-local (:adjusted-pointer-pos db))
                                     (:pressure e)))
        fill (document.handlers/attr db :fill)]
    (if (:shift-key e)
      (assoc db :last-origin (:pointer-pos e))
      (-> db
          (tool.handlers/set-state :create)
          (element.handlers/add {:type :element
                                 :tag :brush
                                 :parent parent-id
                                 :attrs {:points point
                                         :fill fill
                                         :size brush-size
                                         :thinning 0.5
                                         :smoothing 0.5
                                         :streamline 0.5}})))))

(defmethod tool.hierarchy/on-drag [::brush :idle]
  [db e]
  (cond-> db
    (:shift-key e)
    (-> (document.handlers/update-attr
         ::size
         (fn [size]
           (let [{:keys [last-origin]} db
                 [delta-x delta-y] (matrix/sub (:pointer-pos e) last-origin)
                 delta (if (> (abs delta-x) (abs delta-y)) delta-x (- delta-y))]
             (if (pos? delta)
               (min max-size (inc size))
               (max min-size (dec size))))))
        (update-brush-size)
        (assoc :last-origin (:pointer-pos e)))))

(defmethod tool.hierarchy/on-drag [::brush :create]
  [db e]
  (let [{:keys [adjusted-pointer-pos]} db
        selected (first (element.handlers/selected db))
        point (element.handlers/local-point db (:id selected)
                                            adjusted-pointer-pos)
        point (string/join " " (conj point (:pressure e)))]
    (element.handlers/update-selected db
                                      update-in [:attrs :points]
                                      str " " point)))

(defmethod tool.hierarchy/on-drag-end [::brush :create]
  [db e]
  (-> db
      (history.handlers/finalize (:timestamp e) [::draw-brush "Brush"])
      (tool.handlers/deactivate)))

(defmethod tool.hierarchy/on-deactivate ::brush
  [db]
  (app.handlers/enqueue-fx db [::set-brush nil]))

(defmethod tool.hierarchy/render ::brush
  []
  (let [state @(rf/subscribe [::tool.subs/state])]
    (when-not (= :create state)
      [element.hierarchy/render @brush])))

(rf/dispatch [::action.events/register-action
              {:id :tool/brush
               :label [::label "Brush"]
               :icon "brush"
               :event [::tool.events/activate ::brush]
               :active [::tool.subs/active? ::brush]
               :shortcuts {"All" [{:keyCode (utils.key/codes "B")}]}}])
