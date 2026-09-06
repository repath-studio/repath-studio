(ns renderer.history.db
  (:require
   [renderer.db :refer [Vec2]]
   [renderer.element.db :refer [Element ElementId]]
   [renderer.i18n.db :refer [Translation]]))

(def HistoryIndex [:or pos-int? zero?])

(def HistoryState
  [:map {:closed true}
   [:explanation Translation]
   [:timestamp number?]
   [:index HistoryIndex]
   [:elements {:optional true} [:map-of ElementId Element]]
   [:parent {:optional true} HistoryIndex]
   [:children [:vector HistoryIndex]]])

(def History
  [:map {:closed true}
   [:zoom {:optional true} number?]
   [:labels {:optional true} boolean?]
   [:translate {:optional true} Vec2]
   [:position {:optional true} HistoryIndex]
   [:states {:default {}} [:map-of HistoryIndex HistoryState]]])
