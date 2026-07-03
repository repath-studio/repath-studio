(ns renderer.toolbar.views
  (:require
   [renderer.utils.extra :refer [rpartial]]
   [renderer.views :as views]))

(defn action-toolbar
  [{:keys [actions orientation]} & more]
  (let [vertical? (= orientation :vertical)]
    (->> actions
         (map (rpartial views/action-button-group :side "left"))
         (interpose [:span {:class (if vertical? "h-divider" "v-divider")}])
         (into [:<>])
         (conj more)
         (into [views/toolbar
                {:class "flex-col px-2 md:px-1 gap-2 md:gap-1"}]))))
