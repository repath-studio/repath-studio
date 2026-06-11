(ns renderer.attribute.impl.d
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/d"
  (:require
   ["@radix-ui/react-select" :as Select]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.document.events :as-alias document.events]
   [renderer.element.events :as-alias element.events]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.events :as-alias events]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.edit.core :as tool.impl.base.edit]
   [renderer.tool.impl.element.path :as tool.impl.element.path]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.key :as utils.key]
   [renderer.utils.path :as utils.path]
   [renderer.views :as views]))

(defmethod attribute.hierarchy/description [::element.hierarchy/element :d]
  []
  [[::description "The d attribute defines a path to be drawn."]])

(def path-commands
  {"M" {:label [::move-to "Move To"]
        :url "https://svgwg.org/svg2-draft/paths.html#PathDataMovetoCommands"}
   "L" {:label [::line-to "Line To"]
        :url "https://svgwg.org/svg2-draft/paths.html#PathDataLinetoCommands"}
   "V" {:label [::vertical-line "Vertical Line"]
        :url "https://svgwg.org/svg2-draft/paths.html#PathDataLinetoCommands"}
   "H" {:label [::horizontal-line "Horizontal Line"]
        :url "https://svgwg.org/svg2-draft/paths.html#PathDataLinetoCommands"}
   "C" {:label [::cubic-bezier "Cubic Bézier Curve"]
        :url "https://svgwg.org/svg2-draft/paths.html#PathDataCubicBezierCommands"}
   "S" {:label [::shortcut-cubic-bezier "Shortcut Cubic Bézier Curve"]
        :url "https://svgwg.org/svg2-draft/paths.html#PathDataCubicBezierCommands"}
   "Q" {:label [::quadratic-bezier-curve "Quadratic Bézier Curve"]
        :url "https://svgwg.org/svg2-draft/paths.html#PathDataQuadraticBezierCommands"}
   "T" {:label [::shortcut-quadratic "Shortcut Quadratic Bézier Curve"]
        :url "https://svgwg.org/svg2-draft/paths.html#PathDataQuadraticBezierCommands"}
   "A" {:label [::elliptical-arc-curve "Elliptical Arc Curve"]
        :url "https://svgwg.org/svg2-draft/paths.html#PathDataEllipticalArcCommands"}
   "Z" {:label [::close-path "Close Path"]
        :url "https://svgwg.org/svg2-draft/paths.html#PathDataClosePathCommand"}})

(defn ->command
  [c]
  (get path-commands (string/upper-case c)))

(defn segment-id
  [index]
  (keyword (str index) "end-point"))

(defn segment-index
  [id]
  (some-> id namespace js/parseInt))

(defn segment-active?
  [ids index]
  (some #(and (keyword? %)
              (= (segment-index %) index)) ids))

(rf/reg-event-db
 ::drop-segment
 (fn [db [_ el-id index timestamp]]
   (-> db
       (element.handlers/clear-selected-handles el-id)
       (element.handlers/select-handle (segment-id index) el-id)
       (element.handlers/delete-segments)
       (history.handlers/finalize timestamp
                                  [::remove-segment "Remove segment"]))))

(rf/reg-event-db
 ::convert-segment
 (fn [db [_ el-id index command timestamp]]
   (-> db
       (element.handlers/update-attr
        el-id :d #(some-> %
                          (utils.path/string->segments)
                          (utils.path/convert-segment index command)
                          (utils.path/segments->string)))
       (history.handlers/finalize timestamp [::convert-segment
                                             "Convert segment"]))))

(defn set-segment-value
  [e {:keys [d index field-index value]}]
  (let [new-v (.. e -target -value)]
    (if (or (string/blank? new-v) (js/isNaN new-v))
      (set! (.. e -target -value) value)
      (let [segments (utils.path/string->segments d)
            new-seg (.slice (aget segments index))]
        (aset new-seg field-index new-v)
        (let [new-segs (.slice segments)]
          (aset new-segs index new-seg)
          (rf/dispatch [::element.events/set-attr :d
                        (utils.path/segments->string new-segs)]))))))

(defn segment-input
  [{:keys [d index]} {:keys [field-index value id]}]
  (let [idle? @(rf/subscribe [::tool.subs/idle?])
        attrs {:d d
               :index index
               :field-index field-index
               :value value}]
    [:input.form-element.min-w-0
     {:id id
      :key (str index "-" field-index "-" value)
      :default-value value
      :enter-key-hint "done"
      :disabled (not idle?)
      :on-blur #(set-segment-value % attrs)
      :on-pointer-up attribute.views/pointer-up-handler
      :on-key-down #(utils.key/down-handler % value set-segment-value attrs)}]))

(defn segment-field
  [ctx label field-index segment]
  (let [v (nth segment field-index)
        input-id (str "seg-" (:index ctx) "-" field-index)]
    [:<>
     [:label.form-element {:for input-id
                           :title label} label]
     [segment-input ctx {:field-index field-index
                         :value v
                         :id input-id}]]))

(defmulti segment-form (fn [segment _] (-> (first segment)
                                           (string/lower-case)
                                           (keyword))))

(defn col-span
  []
  [:div.col-span-2.bg-primary])

(defn mapped-segment-forms
  [segment ctx field-indices]
  (->> field-indices
       (map-indexed (fn [i label] [segment-field ctx label (inc i) segment]))))

(defmethod segment-form :default
  [segment ctx]
  (mapped-segment-forms segment ctx ["x" "y"]))

(defmethod segment-form :h
  [segment ctx]
  [[segment-field ctx "x" 1 segment]
   [col-span]])

(defmethod segment-form :v
  [segment ctx]
  [[segment-field ctx "y" 1 segment]
   [col-span]])

(defmethod segment-form :z [_segment _ctx])

(defmethod segment-form :c
  [segment ctx]
  (mapped-segment-forms segment ctx ["x1" "y1" "x2" "y2" "x" "y"]))

(defmethod segment-form :s
  [segment ctx]
  (mapped-segment-forms segment ctx ["x2" "y2" "x" "y"]))

(defmethod segment-form :q
  [segment ctx]
  (mapped-segment-forms segment ctx ["x1" "y1" "x" "y"]))

(defmethod segment-form :a
  [segment ctx]
  [[segment-field ctx "rx" 1 segment]
   [segment-field ctx "ry" 2 segment]
   [segment-field ctx "x-rotation" 3 segment]
   [col-span]
   [segment-field ctx "large-arc" 4 segment]
   [col-span]
   [segment-field ctx "sweep" 5 segment]
   [col-span]
   [segment-field ctx "x" 6 segment]
   [segment-field ctx "y" 7 segment]])

(defn command-item
  [[command {:keys [label]}]]
  [:> Select/Item
   {:value command
    :disabled (contains? #{"Z" "M" "A" "T"} (string/upper-case command))
    :class "menu-item"}
   [:> Select/ItemText
    (i18n.views/t label)
    [:div.menu-item-indicator.w-6! command]]])

(defn command-select
  [el-id index command]
  (let [disabled? (contains? #{"M" "Z"} (string/upper-case command))
        {:keys [label]} (->command command)]
    [:> Select/Root
     {:value (string/upper-case command)
      :onValueChange #(rf/dispatch [::convert-segment el-id index %
                                    (js/Date.now)])
      :disabled disabled?}
     [:> Select/Trigger
      {:class "form-control-button flex gap-1 px-2!
               bg-transparent! hover:bg-overlay! text-inherit!"
       :title (i18n.views/t [::convert-segment "Convert segment"])}
      [:> Select/Value {:as-child true}
       [:span.text-ellipsis.overflow-hidden (i18n.views/t label)]]
      (when-not disabled?
        [:> Select/Icon
         [views/icon "chevron-down"]])]
     [:> Select/Portal
      [:> Select/Content
       {:class "menu-content rounded-sm select-content"
        :on-key-down #(.stopPropagation %)
        :on-escape-key-down #(.stopPropagation %)}
       (->> path-commands
            (map command-item)
            (into [:> Select/Viewport {:class "select-viewport"}]))]]]))

(defn segment-delete-button
  [el-id index]
  [:button.form-control-button.bg-transparent!.hover:bg-overlay!.text-inherit!
   {:on-click #(do (.stopPropagation %)
                   (rf/dispatch [::drop-segment el-id index (js/Date.now)]))
    :title (i18n.views/t [::remove-segment "Remove segment"])}
   [views/icon "times"]])

(defn segment-row
  [{:keys [el-id index segment d selected-handles]}]
  (let [command (first segment)
        {:keys [url]} (->command command)
        id (segment-id index)
        hovered? @(rf/subscribe [::element.subs/hovered? id])
        selected? (segment-active? selected-handles index)]
    [:div.flex.flex-col.gap-px.overflow-hidden
     {:on-pointer-enter #(rf/dispatch [::document.events/set-hovered-id id])
      :on-pointer-leave #(rf/dispatch [::document.events/clear-hovered])}
     [:div.flex.overflow-hidden
      [:div.bg-primary.flex-1.flex.p-2.overflow-hidden
       {:on-click #(rf/dispatch [::element.events/toggle-handle-selection
                                 el-id id (.-shiftKey %)])
        :class [(when (and hovered? (not selected?)) "bg-overlay!")
                (when selected? "bg-accent! text-accent-foreground!")]}
       [:div.flex.items-center.gap-2.justify-between.w-full.overflow-hidden
        [:div.flex.overflow-hidden
         [:button.form-control-button.bg-overlay!.text-inherit!
          {:title (i18n.views/t [::open-command-specification
                                 "Open command specification"])
           :on-click #(do (.stopPropagation %)
                          (rf/dispatch [::events/open-remote-url url]))}
          command]
         [command-select el-id index command]]]

       (when (pos? index)
         [:div.flex
          [:button.form-control-button.opacity-50
           {:class "bg-transparent! hover:bg-overlay! text-inherit! px-2!"
            :disabled true}
           (if (= command (string/lower-case command))
             (i18n.views/t [::relative "Relative"])
             (i18n.views/t [::absolute "Absolute"]))]
          [segment-delete-button el-id index]])]]

     (some->> (segment-form segment {:d d
                                     :index index})
              (into [:div.grid.gap-px
                     {:style {:grid-template-columns "38px 1fr auto 1fr"}}]))]))

(defn segments-form
  [segments element]
  (let [v (get-in element [:attrs :d])
        {:keys [id tag selected-handles]} element]
    [:div.flex.flex-col.gap-px
     [attribute.views/heading "d" tag :d]
     (->> segments
          (map-indexed (fn [index segment]
                         ^{:key index}
                         [segment-row {:el-id id
                                       :index index
                                       :segment segment
                                       :d v
                                       :selected-handles selected-handles}]))
          (into [:div.flex.flex-col.gap-px]))]))

(defn edit-form
  []
  (let [selected-elements @(rf/subscribe [::element.subs/selected])
        state @(rf/subscribe [::tool.subs/state])
        element (first selected-elements)
        v (get-in element [:attrs :d])
        segments (utils.path/string->segments v)]
    (if (or (state #{:idle :create})
            (< (count segments) 20))
      [segments-form segments element]
      (reagent/with-let [segments segments
                         element element]
        [segments-form segments element]))))

(defmethod attribute.hierarchy/form-element [::element.hierarchy/element :d]
  [_ k v {:keys [disabled]}]
  (let [idle (= @(rf/subscribe [::tool.subs/state]) :idle)]
    [:div.flex.gap-px.w-full
     [attribute.views/form-input k v
      {:disabled (or disabled (not v) (not idle))}]
     (when v
       [views/icon-button "pencil"
        {:title (i18n.views/t [::edit "Edit path"])
         :class "form-control-button"
         :on-click #(rf/dispatch [::tool.events/edit])
         :disabled disabled}])]))

(defmethod tool.hierarchy/attributes-panel [::tool.impl.base.edit/edit :path]
  []
  [edit-form])

(defmethod tool.hierarchy/attributes-panel [::tool.impl.element.path/path :path]
  []
  [edit-form])
