(ns renderer.attribute.impl.d
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/d"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   ["svgpath" :as svgpath]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.events :as-alias events]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.views :as views]))

(defmethod attribute.hierarchy/description [:default :d]
  []
  [[::description "The d attribute defines a path to be drawn."]])

(defn path-commands
  []
  {:m {:label [::move-to "Move To"]
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataMovetoCommands"}
   :l {:label [::line-to "Line To"]
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataLinetoCommands"}
   :v {:label [::vertical-line "Vertical Line"]
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataLinetoCommands"}
   :h {:label [::horizontal-line "Horizontal Line"]
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataLinetoCommands"}
   :c {:label [::cubic-bezier "Cubic Bézier Curve"]
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataCubicBezierCommands"}
   :s {:label [::shortcut-cubic-bezier "Shortcut Cubic Bézier Curve"]
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataCubicBezierCommands"}
   :q {:label [::quadratic-bezier-curve "Quadratic Bézier Curve"]
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataQuadraticBezierCommands"}
   :t {:label [::shortcut-quadratic "Shortcut Quadratic Bézier Curve"]
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataQuadraticBezierCommands"}
   :a {:label [::elliptical-arc-curve "Elliptical Arc Curve"]
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataEllipticalArcCommands"}
   :z {:label [::close-path "Close Path"]
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataClosePathCommand"}})

(defn ->command
  [c]
  (get (path-commands) (keyword (string/lower-case c))))

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
    [:div.my-2
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
  [v]
  (let [path (-> v svgpath)
        segments (.-segments path)]
    [:div.flex.overflow-hidden
     {:style {:max-height "50vh"}}
     [views/scroll-area
      [:div.p-4.flex.flex-col
       (map-indexed (fn [index segment]
                      ^{:key (str "segment-" index)}
                      [segment-row index segment path]) segments)]]]))

(defmethod attribute.hierarchy/form-element [:default :d]
  [_ k v {:keys [disabled]}]
  (let [idle (= @(rf/subscribe [::tool.subs/state]) :idle)]
    [:div.flex.gap-px.w-full
     [attribute.views/form-input k (if idle v "waiting")
      {:disabled (or disabled (not v) (not idle))}]
     (when v
       [:> Popover/Root {:modal true}
        [:> Popover/Trigger
         {:title (i18n.views/t [::edit "Edit path"])
          :class "form-control-button"
          :disabled disabled}
         [views/icon "pencil"]]
        [:> Popover/Portal
         [:> Popover/Content
          {:sideOffset 5
           :class "popover-content"
           :align "end"
           :on-escape-key-down #(.stopPropagation %)}
          (when idle [edit-form v])
          [views/popover-arrow]]]])]))
