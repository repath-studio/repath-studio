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
   [renderer.utils.key :as utils.key]
   [renderer.views :as views]))

(hierarchy/derive! ::brush ::tool.hierarchy/draw)

(defonce brush (reagent/atom nil))

(rf/reg-fx
 ::set-brush
 (fn [value]
   (reset! brush value)))

(defmethod tool.hierarchy/tool-options ::brush
  []
  (let [brush-size @(rf/subscribe [::document.subs/attr :brush-size])]
    [:div.flex.items-center.gap-2
     [:span
      brush-size]
     [views/slider
      {:min 1
       :max 100
       :step 1
       :title (i18n.views/t [::brush-size "Brush Size"])
       :value [brush-size]
       :class "w-32"
       :on-value-change (fn [[v]]
                          (rf/dispatch [::document.events/set-attr
                                        :brush-size v]))}]]))

(defmethod tool.hierarchy/on-pointer-move [::brush :idle]
  [db e]
  (let [brush-size (document.handlers/attr db :brush-size)
        [x y] (:adjusted-pointer-pos db)
        pressure (:pressure e)
        pressure (if (zero? pressure) 1 pressure)
        r (* (/ brush-size 2) pressure)
        fill (document.handlers/attr db :fill)]
    (app.handlers/add-fx db [::set-brush {:type :element
                                          :tag :circle
                                          :attrs {:cx x
                                                  :cy y
                                                  :r r
                                                  :fill fill}}])))

(defmethod tool.hierarchy/on-drag-start [::brush :idle]
  [db e]
  (let [brush-size (document.handlers/attr db :brush-size)
        point (string/join " " (conj (:adjusted-pointer-pos db) (:pressure e)))
        fill (document.handlers/attr db :fill)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :brush
                               :attrs {:points point
                                       :fill fill
                                       :size brush-size
                                       :thinning 0.5
                                       :smoothing 0.5
                                       :streamline 0.5}}))))

(defmethod tool.hierarchy/on-drag [::brush :create]
  [db e]
  (let [[min-x min-y] (element.handlers/parent-offset db)
        point (matrix/sub (:adjusted-pointer-pos db) [min-x min-y])
        point (string/join " " (conj point (:pressure e)))]
    (element.handlers/update-selected db
                                      update-in [:attrs :points]
                                      str " " point)))

(defmethod tool.hierarchy/on-drag-end [::brush :create]
  [db e]
  (-> db
      (history.handlers/finalize (:timestamp e) [::draw-brush "Draw brush"])
      (tool.handlers/deactivate)))

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
               :shortcuts [{:keyCode (utils.key/codes "B")}]}])
