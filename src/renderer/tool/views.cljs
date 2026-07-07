(ns renderer.tool.views
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-tooltip" :as Tooltip]
   ["react" :as react]
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [malli.core :as m]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.action.views :as action.views]
   [renderer.app.subs :as-alias app.subs]
   [renderer.db :refer [BBox]]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.subs :as-alias element.subs]
   [renderer.i18n.views :as i18n.views]
   [renderer.input.impl.pointer :as input.impl.pointer]
   [renderer.tool.db :refer [Handle]]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.dom :as utils.dom]
   [renderer.views :as views]))

(m/=> handle [:-> Handle any?])
(defn handle
  [el]
  (let [{:keys [position id cursor label rounded implied parent]} el
        clicked-element @(rf/subscribe [::app.subs/clicked-element])
        handle-size @(rf/subscribe [::document.subs/handle-size])
        zoom @(rf/subscribe [::document.subs/zoom])
        selected @(rf/subscribe [::element.subs/handle-selected? parent id])
        selected (or selected (and (= (:id clicked-element) (:id el))
                                   (= (:parent clicked-element) (:parent el))))
        hovered @(rf/subscribe [::element.subs/hovered? id])
        pointer-handler (partial input.impl.pointer/handler! el)
        active (or selected hovered)
        [x y] position
        scale (if hovered 1.3 1)
        half-size (/ handle-size 2)]
    [:g
     [:rect {:style {:transition "transform 0.1s ease-out"}
             :transform (str "scale(" scale ")")
             :transform-origin (string/join " " position)
             :x (- x half-size)
             :y (- y half-size)
             :rx (when rounded half-size)
             :width handle-size
             :height handle-size
             :stroke-opacity ".5"
             :stroke-width (/ 1 zoom)
             :cursor (or cursor "move")
             :pointer-events (when implied "none")
             :on-pointer-up pointer-handler
             :on-pointer-down pointer-handler
             :on-pointer-move pointer-handler
             :fill (cond
                     selected "var(--accent)"
                     implied "lightgray"
                     :else "var(--accent-foreground)")
             :stroke (cond
                       active "var(--accent)"
                       implied "var(--border)"
                       :else "var(--foreground-muted)")}
      (when label [:title (i18n.views/t label)])]]))

(m/=> selected-bbox [:-> BBox any?])
(defn selected-bbox
  [bbox]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        [min-x min-y] bbox
        [w h] (utils.bounds/->dimensions bbox)
        pointer-handler (partial input.impl.pointer/handler! {:type :handle
                                                              :action :translate
                                                              :id :bbox})]
    [:rect {:x min-x
            :y min-y
            :width w
            :height h
            :stroke-opacity ".3"
            :fill "transparent"
            :shape-rendering "crispEdges"
            :stroke-width (/ 2 zoom)
            :on-pointer-up pointer-handler
            :on-pointer-down pointer-handler
            :on-pointer-move pointer-handler}]))

(m/=> min-bbox [:-> BBox BBox])
(defn min-bbox
  "Ensures the bounding box is large enough to avoid overlapping handles."
  [bbox]
  (let [dimensions (utils.bounds/->dimensions bbox)
        [w h] dimensions
        handle-size @(rf/subscribe [::document.subs/handle-size])
        min-size (* handle-size 2)]
    (cond-> bbox
      (< w min-size)
      (matrix/add [(- (/ (- min-size w) 2)) 0
                   (/ (- min-size w) 2) 0])

      (< h min-size)
      (matrix/add [0 (- (/ (- min-size h) 2))
                   0 (/ (- min-size h) 2)]))))

(m/=> corner-handles [:-> BBox any?])
(defn corner-handles
  [bbox]
  (let [idle? @(rf/subscribe [::tool.subs/idle?])
        bbox (cond-> bbox idle? min-bbox)
        [min-x min-y max-x max-y] bbox
        [w h] (utils.bounds/->dimensions bbox)]
    (->> [{:position [min-x min-y]
           :id :top-left
           :cursor "nwse-resize"}
          {:position [max-x min-y]
           :id :top-right
           :cursor "nesw-resize"}
          {:position [min-x max-y]
           :id :bottom-left
           :cursor "nesw-resize"}
          {:position [max-x max-y]
           :id :bottom-right
           :cursor "nwse-resize"}
          {:position [(+ min-x (/ w 2)) min-y]
           :id :top-middle
           :cursor "ns-resize"}
          {:position [max-x (+ min-y (/ h 2))]
           :id :middle-right
           :cursor "ew-resize"}
          {:position [min-x (+ min-y (/ h 2))]
           :id :middle-left
           :cursor "ew-resize"}
          {:position [(+ min-x (/ w 2)) max-y]
           :id :bottom-middle
           :cursor "ns-resize"}]
         (mapv (comp handle
                     (partial merge {:type :handle
                                     :action :scale})))
         (into [:g]))))

(defn tool-action
  ([action]
   (second (:event action)))
  ([actions tool-id]
   (some #(when (= tool-id (tool-action %)) %) actions)))

(defn button
  [action bordered]
  (let [active (action.views/checked? action)
        cached-tool @(rf/subscribe [::tool.subs/cached])
        primary (= cached-tool (tool-action action))]
    [:> Tooltip/Root
     [:> Tooltip/Trigger
      {:as-child true}
      [:span
       [views/radio-icon-button (:icon action) active
        {:class [(when primary "outline outline-inset outline-accent")
                 (when bordered "border border-border")]
         :aria-label (action.views/label action)
         :on-click (action.views/dispatch action)}]]]
     [:> Tooltip/Portal
      [:> Tooltip/Content
       {:class "tooltip-content"
        :sideOffset 10
        :side "top"
        :on-escape-key-down #(.stopPropagation %)}
       [:div.flex.gap-2.items-center
        [action.views/label action]
        [views/shortcuts action]]]]]))

(defn button-group
  [action-group]
  (some->> (:actions action-group)
           (map (fn [action] [button action false]))
           (into [:div {:class "flex justify-center md:gap-1 gap-0.5"}])))

(defn dropdown-button
  [group]
  (let [{:keys [label actions icon]} group
        active-tool @(rf/subscribe [::tool.subs/active])
        cached-tool @(rf/subscribe [::tool.subs/cached])
        active-action (tool-action actions active-tool)
        cached-action (tool-action actions cached-tool)
        top-tool (or active-action cached-action)]
    (if (second actions)
      [:> DropdownMenu/Root
       [:> DropdownMenu/Trigger
        {:as-child true}
        [:div
         [:> Tooltip/Root
          [:> Tooltip/Trigger
           {:as-child true}
           [:button.button.flex.items-center.justify-center.px-2.font-mono
            {:aria-label (i18n.views/t label)
             :class ["rounded-sm gap-1 border border-border"
                     (when cached-action "outline outline-inset outline-accent")
                     (when active-action
                       "bg-accent text-accent-foreground! hover:bg-accent-light
                        aria-expanded:bg-accent-light active:bg-accent-light")]}
            [views/icon (or (:icon top-tool) icon (:icon (first actions)))]
            [views/icon "chevron-down"]]]
          [:> Tooltip/Portal
           [:> Tooltip/Content
            {:class "tooltip-content"
             :sideOffset 10
             :side "top"
             :on-escape-key-down #(.stopPropagation %)}
            [:div.flex.gap-2.items-center
             (i18n.views/t label)]]]]]]

       [:> DropdownMenu/Portal
        (->> actions
             (map views/dropdown-menu-item)
             (into [:> DropdownMenu/Content
                    {:side "bottom"
                     :align "middle"
                     :class "menu-content rounded-sm"
                     :on-key-down #(.stopPropagation %)
                     :on-escape-key-down #(.stopPropagation %)}
                    [views/dropdownmenu-arrow]]))]]
      [button (first actions) true])))

(def action-groups
  [:tools/transform
   :tools/containers
   :tools/elements
   :tools/draw
   :tools/misc
   :tools/extensions])

(defn toolbar
  []
  (let [overflow? (reagent/atom false)
        measure-ref (react/createRef)
        observer (js/ResizeObserver.
                  (fn [_entries]
                    (let [el (.-current measure-ref)]
                      (reset! overflow? (utils.dom/content-overflow? el)))))]
    (reagent/create-class
     {:component-did-mount
      #(.observe observer (.-current measure-ref))

      :component-will-unmount
      #(.disconnect observer)

      :reagent-render
      (fn []
        (let [groups (keep action.views/deref-action-group action-groups)]
          [:div.relative
           (->> groups
                (map button-group)
                (interpose [:span.v-divider])
                (into [views/toolbar
                       {:ref measure-ref
                        :class "absolute invisible w-full overflow-hidden"}]))
           (if @overflow?
             (->> groups
                  (map dropdown-button)
                  (into [views/toolbar
                         {:class "bg-primary justify-center py-2 gap-2"}]))
             (->> groups
                  (map button-group)
                  (interpose [:span.v-divider])
                  (into [views/toolbar
                         {:class "bg-primary justify-center py-2"}])))]))})))
