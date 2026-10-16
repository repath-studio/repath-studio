(ns renderer.tool.impl.draw.brush
  "https://github.com/steveruizok/perfect-freehand"
  (:require
   [clojure.string :as string]
   [malli.core :as m]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.action.events :as-alias action.events]
   [renderer.app.db :refer [App]]
   [renderer.app.handlers :as app.handlers]
   [renderer.db :refer [Orientation Vec2]]
   [renderer.document.events :as-alias document.events]
   [renderer.document.handlers :as document.handlers]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.input.handlers :as input.handlers]
   [renderer.input.subs :as-alias input.subs]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.tool.views :as tool.views]
   [renderer.utils.element :as utils.element]
   [renderer.utils.key :as utils.key]
   [renderer.utils.math :as utils.math]
   [renderer.views :as views]))

(hierarchy/derive! ::brush ::tool.hierarchy/draw)

(defonce brush (reagent/atom nil))

(def min-size 1)
(def max-size 100)
(def default-size 16)

(m/=> locked-axis [:-> Vec2 Orientation])
(defn locked-axis
  "Returns the dominant orientation of a delta vector."
  [[x y]]
  (if (> (abs x) (abs y))
    :horizontal
    :vertical))

(m/=> lock-point [:-> Vec2 Orientation Vec2 Vec2])
(defn lock-point
  [origin orientation [px py]]
  (case orientation
    :horizontal [px (second origin)]
    :vertical [(first origin) py]))

(m/=> direction-determined? [:-> App boolean?])
(defn direction-determined?
  [db]
  (let [{:keys [pointer-offset pointer-pos drag-threshold]} db]
    (> (utils.math/distance pointer-offset pointer-pos)
       (* 5 drag-threshold))))

(rf/reg-fx
 ::set-brush
 (fn [value]
   (reset! brush value)))

(rf/reg-fx
 ::set-brush-size
 (fn [value]
   (swap! brush assoc :size value)))

(rf/reg-fx
 ::set-brush-axis
 (fn [value]
   (swap! brush assoc :lock-axis value)))

(rf/reg-fx
 ::set-brush-origin
 (fn [value]
   (swap! brush assoc :origin value)))

(defn update-brush-size
  [db]
  (let [brush-size (document.handlers/attr db ::size)]
    (app.handlers/enqueue-fx db [::set-brush-size brush-size])))

(defmethod tool.hierarchy/help [::brush :idle]
  []
  (i18n.views/t [::help-idle [:div "Click and drag to brush.
                                    Hold %1 to adjust the brush size,
                                    %2 to restrict direction."]]
                [[views/kbd "⇧"]
                 [tool.views/primary-mod-key]]))

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
  (let [size (or (document.handlers/attr db ::size) default-size)]
    (app.handlers/enqueue-fx db [::set-brush-size size])))

(defmethod tool.hierarchy/on-drag-start [::brush :idle]
  [db e]
  (let [brush-size (or (document.handlers/attr db ::size) default-size)
        parent-id (:id (element.handlers/hovered-svg db))
        to-local (partial element.handlers/container-local-point db parent-id)
        origin (to-local (:local-pointer-pos db))
        point (string/join " " (conj origin (:pressure e)))
        primary-mod? (input.handlers/primary-mod? db e)
        fill (document.handlers/attr db :fill)]
    (if (:shift-key e)
      (-> db
          (assoc :last-origin (:pointer-pos e))
          (app.handlers/enqueue-fx
           [::set-brush-origin (:local-pointer-pos db)]))
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
                                         :streamline 0.5}})
          (app.handlers/enqueue-fx [::set-brush {:size brush-size
                                                 :lock? primary-mod?
                                                 :lock-origin origin
                                                 :lock-axis nil}])))))

(defmethod tool.hierarchy/on-drag [::brush :idle]
  [db e]
  (let [{:keys [pointer-pos shift-key]} e]
    (cond-> db
      shift-key
      (-> (document.handlers/update-attr
           ::size
           (fn [size]
             (let [{:keys [last-origin]} db
                   [delta-x delta-y] (utils.math/v-sub pointer-pos last-origin)
                   delta (if (> (abs delta-x) (abs delta-y))
                           delta-x
                           (- delta-y))]
               (if (pos? delta)
                 (min max-size (inc size))
                 (max min-size (dec size))))))
          (update-brush-size)
          (assoc :last-origin pointer-pos)))))

(defmethod tool.hierarchy/on-drag [::brush :create]
  [db e]
  (let [brush-state @brush
        {:keys [local-pointer-pos]} db
        {:keys [lock? lock-origin axis]} brush-state
        selected (first (element.handlers/selected db))
        point (element.handlers/local-point db (:id selected) local-pointer-pos)
        delta (utils.math/v-sub point lock-origin)
        axis (or axis
                 (when (and lock? (direction-determined? db))
                   (locked-axis delta)))
        point (cond->> point (and lock? axis) (lock-point lock-origin axis))
        point (string/join " " (conj point (:pressure e)))]
    (-> db
        (app.handlers/enqueue-fx [::set-brush-axis axis])
        (element.handlers/update-selected update-in [:attrs :points]
                                          str " " point))))

(defmethod tool.hierarchy/on-drag-end [::brush :idle]
  [db _e]
  (app.handlers/enqueue-fx db [::set-brush-origin nil]))

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
  (let [state @(rf/subscribe [::tool.subs/state])
        {:keys [size origin]} @brush
        [x y] (or origin @(rf/subscribe [::input.subs/local-pointer-pos]))
        hovered-svg @(rf/subscribe [::element.subs/hovered-svg])
        scale (-> hovered-svg utils.element/transform first)
        fill @(rf/subscribe [::document.subs/attr :fill])]
    (when (and (not= :create state) (pos? size) scale)
      [element.hierarchy/render {:type :element
                                 :tag :circle
                                 :attrs {:cx (str x)
                                         :cy (str y)
                                         :r (str (/ (* size scale) 2))
                                         :fill fill}}])))

(rf/dispatch [::action.events/register-action
              {:id :tool/brush
               :label [::label "Brush"]
               :icon "brush"
               :event [::tool.events/activate ::brush]
               :active [::tool.subs/active? ::brush]
               :shortcuts {"All" [{:keyCode (utils.key/codes "B")}]}}])
