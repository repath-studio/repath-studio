(ns renderer.attribute.impl.d
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/d"
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
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
       (element.handlers/select-handle (segment-id index) el-id)
       (element.handlers/delete-segments)
       (history.handlers/finalize timestamp
                                  [::remove-segment "Remove segment"]))))

(defn set-segment-value
  [e {:keys [d index field-index value]}]
  (let [new-v (.. e -target -value)]
    (if (or (string/blank? new-v) (js/isNaN new-v))
      (set! (.. e -target -value) value)
      (let [updated-d (-> d
                          (utils.path/string->segments)
                          (assoc-in [index field-index] new-v)
                          (utils.path/segments->string))]
        (rf/dispatch [::element.events/set-attr :d updated-d])))))

(defn segment-input
  [{:keys [d index]} {:keys [field-index value]}]
  (let [idle? @(rf/subscribe [::tool.subs/idle?])
        attrs {:d d
               :index index
               :field-index field-index
               :value value}]
    [:input.form-element
     {:key (str index "-" field-index "-" value)
      :default-value value
      :enter-key-hint "done"
      :disabled (not idle?)
      :on-blur #(set-segment-value % attrs)
      :on-pointer-up attribute.views/pointer-up-handler
      :on-key-down #(utils.key/down-handler % value set-segment-value attrs)}]))

(defn segment-field
  [ctx label field-index segment]
  (let [v (nth segment 1)]
    [:<>
     [:label.form-element.px-1 label]
     [segment-input ctx {:field-index field-index
                         :value v}]]))

(defmulti segment-form (fn [segment _] (-> (first segment)
                                           (string/lower-case)
                                           (keyword))))

(defmethod segment-form :default
  [segment ctx]
  [:div.grid.grid-cols-4.gap-px
   [segment-field ctx "x" 1 segment]
   [segment-field ctx "y" 2 segment]])

(defmethod segment-form :h
  [segment ctx]
  [:div.grid.grid-cols-2.gap-px
   [segment-field ctx "x" 1 segment]])

(defmethod segment-form :v
  [segment ctx]
  [:div.grid.grid-cols-2.gap-px
   [segment-field ctx "y" 1 segment]])

(defmethod segment-form :z [_segment _ctx])

(defmethod segment-form :a
  [segment ctx]
  [:div
   [:div.grid.grid-cols-4.gap-px
    [segment-field ctx "rx" 1 segment]
    [segment-field ctx "ry" 2 segment]]
   [:div.grid.grid-cols-2.gap-px
    [segment-field ctx "x-axis-rotation" 3 segment]]
   [:div.grid.grid-cols-2.gap-px
    [segment-field ctx "large-arc-flag" 4 segment]]
   [:div.grid.grid-cols-2.gap-px
    [segment-field ctx "sweep-flag" 5 segment]]
   [:div.grid.grid-cols-4.gap-px
    [segment-field ctx "x" 6 segment]
    [segment-field ctx "y" 7 segment]]])

(defn segment-row
  [{:keys [el-id index segment d selected-handles]}]
  (let [hovered-ids @(rf/subscribe [::document.subs/hovered-ids])
        command (first segment)
        {:keys [label url]} (->command command)
        id (segment-id index)
        hovered? (segment-active? hovered-ids index)
        selected? (segment-active? selected-handles index)]
    [:div.bg-primary.p-2
     {:on-pointer-enter #(rf/dispatch [::document.events/set-hovered-id id])
      :on-pointer-leave #(rf/dispatch [::document.events/clear-hovered])
      :class [(when (and hovered? (not selected?)) "bg-overlay")
              (when selected? "bg-accent text-accent-foreground")]}
     [:div.flex.items-center.justify-between.mb-1
      [:span
       [:button.p-1.text-inherit
        {:on-click #(rf/dispatch [::element.events/toggle-handle-selection
                                  el-id id (.-shiftKey %)])
         :class ["font-mono"
                 (when selected? "bg-accent-foreground/10 rounded")]}
        (first segment)]
       [:button.p-1.text-inherit
        {:on-click #(rf/dispatch [::events/open-remote-url url])}
        (i18n.views/t label)]
       (if (= command (string/lower-case command))
         (i18n.views/t [::relative "(Relative)"])
         (i18n.views/t [::absolute "(Absolute)"]))]
      [:button.icon-button.small.bg-transparent.text-foreground-muted
       {:on-click #(rf/dispatch [::drop-segment el-id index (.-timestamp %)])
        :disabled (zero? index)
        :title (if (zero? index)
                 (i18n.views/t [::move-required
                                "The initial move segment cannot be removed"])
                 (i18n.views/t [::remove-segment "Remove segment"]))}
       [views/icon "times"]]]
     [segment-form segment {:d d
                            :index index}]]))

(defn edit-form
  []
  (let [selected-elements @(rf/subscribe [::element.subs/selected])
        element (first selected-elements)
        v (get-in element [:attrs :d])
        segments (utils.path/string->segments v)
        {:keys [id tag selected-handles]} element]
    [:div.flex.flex-col.gap-px
     [attribute.views/heading "d" tag :d]
     [:div.flex.overflow-hidden
      {:style {:max-height "50vh"}}
      [views/scroll-area
       [:div.flex.flex-col.gap-px
        (map-indexed (fn [index segment]
                       ^{:key (str segment)}
                       [segment-row {:el-id id
                                     :index index
                                     :segment segment
                                     :d v
                                     :selected-handles selected-handles}])
                     segments)]]]]))

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
