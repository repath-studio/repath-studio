(ns renderer.tool.impl.base.transform.select
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.app.db :refer [App]]
   [renderer.app.handlers :as app.handlers]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.input.handlers :as input.handlers]
   [renderer.tool.db :refer [Handle]]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.transform.core :as-alias transform]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.svg :as utils.svg]
   [renderer.views :as views]))

(defmethod tool.hierarchy/help [::transform/transform :select]
  []
  (i18n.views/t [::select [:div "Hold %1 to select intersecting elements."]]
                [[views/kbd "Alt"]]))

(defonce select-box (reagent/atom nil))

(rf/reg-fx
 ::set-select-box
 (fn [value]
   (reset! select-box value)))

(m/=> selectable? [:-> [:or Element Handle nil?] boolean?])
(defn selectable?
  [el]
  (and el
       (not (:selected el))
       (not= :handle (:type el))
       (not= :canvas (:tag el))))

(m/=> select-element [:-> App boolean? App])
(defn select-element
  [db multiple]
  (let [{:keys [clicked-element]} db]
    (cond-> db
      (selectable? clicked-element)
      (element.handlers/toggle-selection (:id clicked-element) multiple))))

(m/=> hovered? [:-> Element boolean? boolean?])
(defn hovered?
  [el intersecting?]
  (or (when-let [selection-bbox (element.hierarchy/bbox @select-box)]
        (when-let [el-bbox (:bbox el)]
          (if intersecting?
            (utils.bounds/intersect? el-bbox selection-bbox)
            (utils.bounds/contained? el-bbox selection-bbox))))
      false))

(m/=> reduce-by-area [:-> App boolean? ifn? App])
(defn reduce-by-area
  [db e f]
  (let [{:keys [alt-key]} e
        intersecting? (or alt-key (input.handlers/multi-touch? db))]
    (transduce
     (comp
      (element.handlers/visible)
      (filter #(hovered? % intersecting?))
      (map :id))
     (fn [db id]
       (cond-> db
         id (f id)))
     db (element.handlers/entities db))))

(defn render-select-box
  []
  [element.hierarchy/render @select-box])

(defn clear-select-box
  [db]
  (app.handlers/add-fx db [::set-select-box nil]))

(m/=> select-rect [:-> App boolean? App])
(defn select-rect
  [db intersecting?]
  (cond-> (utils.svg/select-box db)
    (not intersecting?)
    (assoc-in [:attrs :fill] "transparent")))

(defmethod tool.hierarchy/on-drag [::transform/transform :select]
  [db e]
  (let [{:keys [alt-key]} e]
    (-> db
        (element.handlers/clear-hovered)
        (app.handlers/add-fx [::set-select-box (select-rect db alt-key)])
        (reduce-by-area e element.handlers/hover))))

(defmethod tool.hierarchy/on-drag-end [::transform/transform :select]
  [db e]
  (cond-> db
    (not (:shift-key e))
    element.handlers/deselect

    :always
    (-> (reduce-by-area e element.handlers/select)
        (clear-select-box)
        (dissoc :clicked-element :pivot-point)
        (tool.handlers/set-state :idle)
        (history.handlers/finalize (:timestamp e)
                                   [::modify-selection "Modify selection"]))))

(defmethod tool.hierarchy/snapping-points [::transform/transform :select]
  [db]
  (let [selected (element.handlers/selected db)
        options (-> db :snap :options)]
    (cond
      (not= (:state db) :idle)
      (cond-> (element.handlers/snapping-points db (filter :visible selected))
        (seq (rest selected))
        (into (utils.bounds/->snapping-points (element.handlers/bbox db)
                                              options))))))

(defmethod tool.hierarchy/snapping-elements [::transform/transform :select]
  [db]
  (element.handlers/non-selected-visible db))
