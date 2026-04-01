(ns renderer.toolbar.views
  (:require
   ["@radix-ui/react-tooltip" :as Tooltip]
   [re-frame.core :as rf]
   [renderer.action.subs :as-alias action.subs]
   [renderer.action.views :as action.views]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(defn button
  [id]
  (when-let [action @(rf/subscribe [::action.subs/entity id])]
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
        (action.views/label action)
        (when @(rf/subscribe [::window.subs/xl?])
          [views/shortcuts action])]]]]))
