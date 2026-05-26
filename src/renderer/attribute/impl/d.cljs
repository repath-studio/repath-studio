(ns renderer.attribute.impl.d
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/d"
  (:require
   ["svgpath" :as svgpath]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.events :as-alias events]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.edit.core :as tool.impl.base.edit]
   [renderer.tool.impl.element.path :as tool.impl.element.path]
   [renderer.tool.subs :as-alias tool.subs]
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

(defn remove-segment-by-index
  [path i]
  (set! (.-segments path) (.splice (.-segments path) i 1))
  (rf/dispatch [::element.events/set-attr :p (.toString path)]))

(defmulti segment-form (fn [segment _] (-> (first segment)
                                           (string/lower-case)
                                           (keyword))))

(defmethod segment-form :default
  [segment index]
  [:div.grid.grid-cols-4.gap-px
   [:label.form-element.px-1 "x"]
   [:input.form-element
    {:key (str "x-" index)
     :default-value (nth segment 1)}]
   [:label.form-element.px-1 "y"]
   [:input.form-element
    {:key (str "y-" index)
     :default-value (nth segment 2)}]])

(defmethod segment-form :h
  [segment index]
  [:input.form-element {:key (str "width-" index)
                        :default-value (nth segment 1)}])

(defmethod segment-form :v
  [segment index]
  [:input.form-element {:key (str "height-" index)
                        :default-value (nth segment 1)}])

(defmethod segment-form :z [_segment _index])

(defmethod segment-form :a
  [segment index]
  [:div
   [:div.grid.grid-cols-4.gap-px
    [:label.form-element.px-1 "rx"]
    [:input.form-element
     {:key (str "rx-" index)
      :default-value (nth segment 1)}]
    [:label.form-element.px-1 "ry"]
    [:input.form-element
     {:key (str "ry-" index)
      :default-value (nth segment 2)}]]
   [:div.grid.grid-cols-2.gap-px
    [:label.form-element.px-1.text-nowrap "x-axis-rotation"]
    [:input.form-element
     {:key (str "x-axis-rotation-" index)
      :default-value (nth segment 3)}]]
   [:div.grid.grid-cols-2.gap-px
    [:label.form-element.px-1.text-nowrap "large-arc-flag"]
    [:input.form-element
     {:key (str "large-arc-flag-" index)
      :default-value (nth segment 4)}]]
   [:div.grid.grid-cols-2.gap-px
    [:label.form-element.px-1.text-nowrap "sweep-flag"]
    [:input.form-element
     {:key (str "sweep-flag" index)
      :default-value (nth segment 5)}]]
   [:div.grid.grid-cols-4.gap-px
    [:label.form-element.px-1 "x"]
    [:input.form-element
     {:key (str "x-" index)
      :default-value (nth segment 6)}]
    [:label.form-element.px-1. "y"]
    [:input.form-element
     {:key (str "y-" index)
      :default-value (nth segment 7)}]]])

(defn segment-row
  [index segment path]
  (let [command (first segment)
        {:keys [label url]} (->command command)]
    [:div.bg-primary.p-2
     #_[:div (string/join " " segment)]
     [:div.flex.items-center.justify-between.mb-1
      [:span
       [:span.bg-primary.p-1 (first segment)]
       [:button.p-1.text-inherit
        {:on-click #(rf/dispatch [::events/open-remote-url url])}
        (i18n.views/t label)]
       (if (= command (string/lower-case command))
         (i18n.views/t [::relative "(Relative)"])
         (i18n.views/t [::absolute "(Absolute)"]))]
      [:button.icon-button.small.bg-transparent.text-foreground-muted
       {:on-click #(remove-segment-by-index path index)}
       [views/icon "times"]]]
     [segment-form segment index]]))

(defn edit-form
  []
  (let [selected-elements @(rf/subscribe [::element.subs/selected])
        element (first selected-elements)
        v (get-in element [:attrs :d])
        path (-> v svgpath)
        segments (.-segments path)]
    [:div.flex.flex-col.gap-px
     [attribute.views/heading "d" :d]
     [:div.flex.overflow-hidden
      {:style {:max-height "50vh"}}
      [views/scroll-area
       [:div.flex.flex-col.gap-px
        (map-indexed (fn [index segment]
                       ^{:key (str segment)}
                       [segment-row index segment path]) segments)]]]]))

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
