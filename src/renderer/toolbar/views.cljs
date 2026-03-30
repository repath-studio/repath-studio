(ns renderer.toolbar.views
  (:require
   ["@radix-ui/react-tooltip" :as Tooltip]
   [re-frame.core :as rf]
   [renderer.action.subs :as-alias action.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.views :as views]))

(defn button
  [{:keys [label icon enabled event]}]
  [:> Tooltip/Root
   [:> Tooltip/Trigger
    {:as-child true}
    [:span.shadow-4
     [views/icon-button icon {:disabled (some-> enabled rf/subscribe deref not)
                              :aria-label (i18n.views/t label)
                              :on-click #(rf/dispatch event)}]]]
   [:> Tooltip/Portal
    [:> Tooltip/Content
     {:class "tooltip-content"
      :side "left"
      :sideOffset 5
      :on-escape-key-down #(.stopPropagation %)}
     [:div.flex.gap-2.items-center
      (i18n.views/t label)
      [views/shortcuts event]]]]])

(defn action-button
  [id]
  (if (= id :divider)
    [:span.h-divider]
    (when-let [action @(rf/subscribe [::action.subs/action id])]
      [button action])))
