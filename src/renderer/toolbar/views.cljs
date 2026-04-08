(ns renderer.toolbar.views
  (:require
   ["@radix-ui/react-tooltip" :as Tooltip]
   [renderer.action.views :as action.views]
   [renderer.views :as views]))

(defn button
  [action]
  [:> Tooltip/Root
   [:> Tooltip/Trigger
    {:as-child true}
    [:span
     [views/action-icon-button action]]]
   [:> Tooltip/Portal
    [:> Tooltip/Content
     {:class "tooltip-content"
      :side "left"
      :sideOffset 5
      :on-escape-key-down #(.stopPropagation %)}
     [:div.flex.gap-2.items-center
      [action.views/label action]
      [views/shortcuts action]]]]])

(defn button-group
  [action-group]
  (->> action-group
       :actions
       (map button)
       (into [:<>])))

(defn action-toolbar
  [{:keys [actions orientation]} & more]
  (let [vertical? (= orientation :vertical)]
    (->> actions
         (map (comp button-group action.views/deref-action-group))
         (interpose [:span {:class (if vertical? "h-divider" "v-divider")}])
         (conj more)
         (into [views/toolbar
                {:class "flex-col px-2 md:px-1 gap-2 md:gap-1"}]))))
