(ns renderer.attribute.impl.points
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/points"
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.document.events :as-alias document.events]
   [renderer.element.events :as-alias element.events]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.edit.core :as tool.impl.base.edit]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.key :as utils.key]
   [renderer.utils.vec :as utils.vec]
   [renderer.views :as views]))

(defmethod attribute.hierarchy/description [::element.hierarchy/element :points]
  []
  [::description
   ["The points attribute defines a list of points. Each point is defined by a
     pair of number representing a X and a Y coordinate in the user coordinate
     system. If the attribute contains an odd number of coordinates, the last
     one will be ignored."]])

(rf/reg-event-db
 ::remove-points
 (fn [db [_ el-id indexes timestamp]]
   (let [v (get-in (element.handlers/entity db el-id) [:attrs :points])
         points (utils.attribute/points->vec v)
         points (->> indexes
                     (reduce utils.vec/remove-nth points)
                     (flatten)
                     (string/join " "))]
     (-> db
         (element.handlers/assoc-prop el-id :selected-handles #{})
         (element.handlers/set-attr el-id :points points)
         (history.handlers/finalize timestamp
                                    [::remove-points "Remove Points"])))))

(defn set-point
  [e {:keys [index points value axis]}]
  (let [new-v (.. e -target -value)]
    (if (js/isNaN new-v)
      (set! (.. e -target -value) value)
      (let [prev-point (nth points index)
            new-point (if (= axis :x)
                        [new-v (second prev-point)]
                        [(first prev-point) new-v])
            points (assoc points index new-point)
            points (->> points
                        (flatten)
                        (string/join " "))]
        (rf/dispatch [::element.events/set-attr :points points])))))

(defn input
  [index v points axis]
  (let [idle? @(rf/subscribe [::tool.subs/idle?])
        point-attrs {:index index
                     :points points
                     :value v
                     :axis axis}]
    [:input.form-element
     {:key (str axis index)
      :default-value v
      :aria-label (name axis)
      :enter-key-hint "done"
      :disabled (not idle?)
      :on-blur #(set-point % point-attrs)
      :on-pointer-up attribute.views/pointer-up-handler
      :on-key-down #(utils.key/down-handler % set-point point-attrs)}]))

(defn point-row
  [el-id index [x y] points]
  (let [handle-id (keyword (str index))
        hovered? @(rf/subscribe [::element.subs/hovered? handle-id])
        selected? @(rf/subscribe [::element.subs/handle-selected?
                                  el-id handle-id])]
    [:div.grid.grid-flow-col.gap-px
     {:dir "ltr"
      :on-pointer-enter #(rf/dispatch [::document.events/set-hovered-id
                                       handle-id])
      :on-pointer-leave #(rf/dispatch [::document.events/clear-hovered])
      :style {:grid-template-columns "minmax(0, 60px) 3fr 3fr 27px"}}
     [:span.form-element.flex-1.py-0!.h-full!.px-4!
      {:on-click #(rf/dispatch [::element.events/toggle-handle-selection
                                el-id handle-id (.-shiftKey %)])
       :class ["leading-[27px]"
               (when (and hovered? (not selected?)) "bg-overlay!")
               (when selected? "bg-accent! text-accent-foreground!")]}
      index]
     [input index x points :x]
     [input index y points :y]
     [views/icon-button "times"
      {:class "form-control-button rounded-none"
       :title (i18n.views/t [::remove-point "Remove point"])
       :on-click #(rf/dispatch [::remove-points
                                el-id #{index} (.-timestamp %)])}]]))

(defn points-form
  []
  (let [selected-elements @(rf/subscribe [::element.subs/selected])
        element (first selected-elements)
        v (get-in element [:attrs :points])
        points (utils.attribute/points->vec v)]
    [:div.flex.flex-col.gap-px
     [attribute.views/heading "points" (:tag element) :points]

     [:div.flex.flex-col.gap-px
      (map-indexed (fn [index point]
                     ^{:key (str index point)}
                     [point-row (:id element) index point points]) points)]]))

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
         :on-click #(rf/dispatch [::tool.events/edit])
         :disabled disabled}])]))

(defmethod tool.hierarchy/attributes-panel
  [::tool.impl.base.edit/edit ::element.hierarchy/poly]
  []
  [points-form])

(defmethod tool.hierarchy/attributes-panel
  [::tool.hierarchy/poly ::element.hierarchy/poly]
  []
  [points-form])
