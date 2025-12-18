(ns renderer.panel.views
  (:require
   ["react-resizable-panels" :refer [Group Panel Separator useDefaultLayout]]
   [reagent.core :refer [defc]]))

(defn separator []
  [:> Separator
   {:class "panel-separator"}])

(defn panel
  [props & children]
  (into [:> Panel props] children))

(defc group
  [{:keys [id]
    :as props} & children]
  (let [{:keys [defaultLayout onLayoutChange]} (useDefaultLayout
                                                #js {:groupId id,
                                                     :storage js/localStorage})]
    (into [:> Group
           (merge {:defaultLayout defaultLayout
                   :onLayoutChange onLayoutChange}
                  props)]
          children)))
