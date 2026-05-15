(ns renderer.attribute.impl.points
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/points"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.vec :as utils.vec]
   [renderer.views :as views]))

(defmethod attribute.hierarchy/description [::element.hierarchy/element :points]
  []
  [::description
   ["The points attribute defines a list of points. Each point is defined by a
     pair of number representing a X and a Y coordinate in the user coordinate
     system. If the attribute contains an odd number of coordinates, the last
     one will be ignored."]])

(defn remove-nth
  [points index]
  (let [points (->> (utils.vec/remove-nth points index)
                    (flatten)
                    (string/join " "))]
    (rf/dispatch [::element.events/set-attr :points points])))

(defn point-row
  [index [x y] points]
  [:div.grid.grid-flow-col.gap-px
   {:dir "ltr"
    :style {:grid-template-columns "minmax(0, 40px) 3fr 3fr 27px"}}
   [:label.form-element.px-1.bg-transparent index]
   [:input.form-element.bg-transparent
    {:key (str "x-" index)
     :default-value x
     :disabled true
     :on-pointer-up attribute.views/pointer-up-handler!}]
   [:input.form-element.bg-transparent
    {:key (str "y-" index)
     :default-value y
     :disabled true
     :on-pointer-up attribute.views/pointer-up-handler!}]
   [:button.button.bg-transparent.text-foreground-muted.h-full.rounded
    {:on-click #(remove-nth points index)}
    [views/icon "times"]]])

(defn points-popover
  [points disabled]
  [:> Popover/Root {:modal true}
   [:> Popover/Trigger
    {:title (i18n.views/t [::edit-points "Edit points"])
     :class "form-control-button"
     :disabled disabled}
    [views/icon "pencil"]]
   [:> Popover/Portal
    [:> Popover/Content
     {:sideOffset 5
      :class "popover-content"
      :align "end"
      :on-escape-key-down #(.stopPropagation %)}
     [:div.flex.overflow-hidden
      {:style {:max-height "50vh"}}
      [views/scroll-area
       [:div.p-4.flex.flex-col.gap-px
        (map-indexed (fn [index point]
                       ^{:key (str "point-" index)}
                       [point-row index point points]) points)]]]
     [views/popover-arrow]]]])

(defmethod attribute.hierarchy/form-element [::element.hierarchy/element
                                             :points]
  [_ k v {:keys [disabled]}]
  (let [state-idle (= @(rf/subscribe [::tool.subs/state]) :idle)]
    [:div.flex.gap-px.w-full
     [attribute.views/form-input k v
      {:disabled (or disabled
                     (not v)
                     (not state-idle))}]
     (when v [points-popover (utils.attribute/points->vec v) disabled])]))
