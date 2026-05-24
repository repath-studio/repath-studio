(ns renderer.attribute.impl.points
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/points"
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.edit :as tool.impl.base.edit]
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
  (let [clicked-element @(rf/subscribe [::app.subs/clicked-element])
        handle-id (keyword (str index))]
    [:div.grid.grid-flow-col.gap-px.bg-primary
     {:dir "ltr"
      :style {:grid-template-columns "minmax(0, 40px) 3fr 3fr 27px"}}
     [:label.form-element.px-1
      {:class (when (= handle-id (:id clicked-element))
                "bg-accent! text-accent-foreground!")}
      index]
     [:input.form-element
      {:key (str "x-" index)
       :default-value x
       :disabled true
       :on-pointer-up attribute.views/pointer-up-handler!}]
     [:input.form-element
      {:key (str "y-" index)
       :default-value y
       :disabled true
       :on-pointer-up attribute.views/pointer-up-handler!}]
     [views/icon-button "times" {:on-click #(remove-nth points index)}]]))

(defn points-form
  [points]
  [:div.flex.flex-col.gap-px
   (map-indexed (fn [index point]
                  ^{:key (str index point)}
                  [point-row index point points]) points)])

(defmethod attribute.hierarchy/form-element [::element.hierarchy/element
                                             :points]
  [_ k v {:keys [disabled]}]
  (let [state-idle (= @(rf/subscribe [::tool.subs/state]) :idle)]
    [:div.flex.gap-px.w-full
     [attribute.views/form-input k v
      {:disabled (or disabled
                     (not v)
                     (not state-idle))}]
     (when v
       [views/icon-button "pencil"
        {:title (i18n.views/t [::edit-points "Edit points"])
         :class "form-control-button"
         :on-click #(rf/dispatch [::tool.events/activate
                                  ::tool.impl.base.edit/edit])
         :disabled disabled}])]))

(defmethod tool.hierarchy/attributes-panel [::tool.impl.base.edit/edit
                                            ::element.hierarchy/poly]
  []
  (let [selected-elements @(rf/subscribe [::element.subs/selected])
        element (first selected-elements)
        v (get-in element [:attrs :points])]
    [:div.flex.flex-col.gap-px
     [:div.flex.bg-primary.py-5.px-4.gap-1.items-center
      [:h1.flex-1.text-lg.overflow-hidden.text-ellipsis.button-size "points"]]
     [points-form (utils.attribute/points->vec v)]]))
