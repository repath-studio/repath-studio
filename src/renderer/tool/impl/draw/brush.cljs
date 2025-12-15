(ns renderer.tool.impl.draw.brush
  "https://github.com/steveruizok/perfect-freehand"
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.app.handlers :as app.handlers]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]))

(defmethod tool.hierarchy/properties :brush
  []
  {:icon "brush"
   :label [::label "Brush"]})

(defonce brush (reagent/atom nil))

(rf/reg-fx
 ::set-brush
 (fn [value]
   (reset! brush value)))

(defmethod tool.hierarchy/on-pointer-move :brush
  [db e]
  (let [[x y] (:adjusted-pointer-pos db)
        pressure (:pressure e)
        pressure (if (zero? pressure) 1 pressure)
        r (* (/ 16 2) pressure)
        stroke (document.handlers/attr db :stroke)]
    (app.handlers/add-fx db [::set-brush {:type :element
                                          :tag :circle
                                          :attrs {:cx x
                                                  :cy y
                                                  :r r
                                                  :fill stroke}}])))

(defmethod tool.hierarchy/on-drag-start :brush
  [db e]
  (let [point (string/join " " (conj (:adjusted-pointer-pos db) (:pressure e)))
        stroke (document.handlers/attr db :stroke)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :brush
                               :attrs {:points point
                                       :stroke stroke
                                       :size 16
                                       :thinning 0.5
                                       :smoothing 0.5
                                       :streamline 0.5}}))))

(defmethod tool.hierarchy/on-drag :brush
  [db e]
  (let [[min-x min-y] (element.handlers/parent-offset db)
        point (matrix/sub (:adjusted-pointer-pos db) [min-x min-y])
        point (string/join " " (conj point (:pressure e)))]
    (element.handlers/update-selected db
                                      update-in [:attrs :points]
                                      str " " point)))

(defmethod tool.hierarchy/on-drag-end :brush
  [db _e]
  (-> db
      (history.handlers/finalize [::draw-brush "Draw brush"])
      (tool.handlers/activate :transform)))

(defmethod tool.hierarchy/render :brush
  []
  (when-not (= :create @(rf/subscribe [::tool.subs/state]))
    [element.hierarchy/render @brush]))
