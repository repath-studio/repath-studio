(ns renderer.attribute.views
  (:require
   ["@radix-ui/react-hover-card" :as HoverCard]
   ["@radix-ui/react-select" :as Select]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.events :as-alias events]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.key :as utils.key]
   [renderer.views :as views]))

(defn browser-icon
  [browser]
  (case browser
    :firefox_android "firefox"
    :chrome_android "chrome"
    :opera_android "opera"
    :safari_ios "safari"
    :webview_ios "safari"
    (name browser)))

(defn browser-support
  [browser version-added]
  [:div.text-center.flex-1
   [:div {:title browser}
    [views/icon (browser-icon browser)]]
   [:div.text-2xs.mt-1.text-primary
    (case version-added
      true [:div.bg-success (i18n.views/t [::all "all"])]
      false [:div.bg-error "x"]
      nil [:div.bg-warning "?"]
      [:div.bg-success (str "≥" version-added)])]])

(defn browser-compatibility
  [support-data]
  [:<>
   [:h4.font-bold.mb-1
    (i18n.views/t [::browser-compatibility "Browser compatibility"])]
   [views/scroll-area
    [:div.flex.mb-4.gap-px
     (for [[browser {:keys [version_added]}] support-data]
       ^{:key browser}
       [browser-support browser version_added])]]])

(defn info-button
  ([url]
   [info-button url [::learn-more "Learn more"]])
  ([url label]
   [:button.button.px-3.flex-1.rounded
    {:on-click #(rf/dispatch [::events/open-remote-url url])}
    (i18n.views/t label)]))

(defn construct-mdn-url
  [attr]
  (str "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/"
       attr))

(defn caniusethis
  [{:keys [tag attr]}]
  (let [data (if attr
               (utils.attribute/compatibility tag attr)
               (utils.attribute/compatibility tag))
        support-data (:support data)
        property (when attr (utils.attribute/property-data-memo attr))
        spec-url (or (:spec_url data)
                     (:href property))
        spec-url (if (vector? spec-url)
                   (first spec-url)
                   spec-url)
        mdn-url (or (when data
                      (or (:mdn_url data)
                          (construct-mdn-url (name attr))))
                    (:mdn_url property))]
    [:div.flex.flex-col
     (when (some :version_added (vals support-data))
       [browser-compatibility support-data])
     [:div.flex.gap-2
      (when mdn-url
        [info-button mdn-url])
      (when spec-url
        [info-button spec-url [::specification "Specification"]])]]))

(defn update-handler!
  ([event k old-v]
   (update-handler! event k old-v true))
  ([event k old-v finalize?]
   (let [new-v (.. event -target -value)]
     (when-not (= new-v old-v)
       (rf/dispatch [(if finalize?
                       ::element.events/set-attr
                       ::element.events/preview-attr) k new-v])))))

(defn pointer-up-handler!
  [event]
  (let [target (.-target event)
        start-pos (.-selectionStart target)
        end-pos (.-selectionEnd target)]
    (when (= start-pos end-pos)
      (.select target))))

(defn form-input
  [k v {:keys [disabled placeholder]
        :as attrs}]
  [:div.relative.flex.flex-1.group
   [:input.form-element
    (merge attrs
           {:key v
            :dir "ltr"
            :class "rtl:text-right"
            :id (name k)
            :default-value v
            :enter-key-hint "done"
            :on-pointer-up pointer-up-handler!
            :placeholder (if v placeholder "multiple")
            :on-blur #(update-handler! % k v)
            :on-key-down #(utils.key/down-handler! % v update-handler! k v)})]
   (when-not (or (empty? (str v)) disabled)
     [:button.form-control-button.bg-primary!.absolute!.right-0.p1.invisible
      {:class "hover:bg-transparent rtl:right-auto rtl:left-0
               group-hover:visible"
       :on-click #(rf/dispatch [::element.events/remove-attr k])}
      [views/icon "times"]])])

(defmethod attribute.hierarchy/form-element :default
  [_ k v {:keys [disabled placeholder]}]
  [form-input k v {:disabled disabled
                   :placeholder (if v placeholder "multiple")}])

(defn range-input
  [k v {:keys [placeholder disabled]
        :as attrs}]
  [:div.flex.flex-1.gap-px
   [form-input k v {:disabled disabled
                    :placeholder placeholder
                    :class "font-mono w-20"}]
   [:div.px-1.flex-1.bg-primary
    [views/slider
     (merge
      attrs
      {:value [(if (empty? v) placeholder v)]
       :on-value-change (fn [[v]]
                          (rf/dispatch [::element.events/preview-attr k v]))
       :on-value-commit (fn [[v]]
                          (rf/dispatch [::element.events/set-attr k v]))})]]])

(defn select-item
  [{:keys [icon label value]}]
  [:> Select/Item
   {:value value
    :class "menu-item"}
   (when icon
     [:div.absolute.left-2 [views/icon icon]])
   [:> Select/ItemText (i18n.views/t label)]])

(defn select-input
  [k v {:keys [disabled items default-value]
        :as attrs}]
  [:div.flex.w-full.gap-px
   [form-input k v (select-keys attrs [:disabled :placeholder])]
   (when (seq items)
     [:> Select/Root
      {:value (if (empty? v) default-value v)
       :onValueChange #(rf/dispatch [::element.events/set-attr k %])
       :disabled disabled}
      [:> Select/Trigger
       {:class "form-control-button"
        :aria-label (i18n.views/t [::select "Select %1"] [(name k)])}
       [:> Select/Value ""]
       [:> Select/Icon
        [views/icon "chevron-down"]]]
      [:> Select/Portal
       [:> Select/Content
        {:class "menu-content rounded-sm select-content"
         :on-key-down #(.stopPropagation %)
         :on-escape-key-down #(.stopPropagation %)}
        [:> Select/ScrollUpButton
         {:class "select-scroll-button"}
         [views/icon "chevron-up"]]
        (into [:> Select/Viewport
               {:class "select-viewport"}]
              (map select-item items))
        [:> Select/ScrollDownButton
         {:class "select-scroll-button"}
         [views/icon "chevron-down"]]]]])])

(defn feature
  [property {:keys [id label]}]
  (when-let [v (get property id)]
    [:<>
     [:h3.font-bold (i18n.views/t label)]
     [:p (cond->> v
           (vector? v)
           (string/join " | "))]]))

(def features
  [{:id :appliesto
    :label [::applies-to "Applies to"]}
   {:id :computed
    :label [::computed "Computed"]}
   {:id :percentages
    :label [::percentages "Percentages"]}
   {:id :animatable
    :label [::animatable "Animatable"]}
   {:id :animationType
    :label [::animation-type "Animation Type"]}
   {:id :syntax
    :label [::syntax "Syntax"]}])

(defn title
  [tag k]
  (let [clicked-element @(rf/subscribe [::app.subs/clicked-element])
        property (utils.attribute/property-data-memo k)
        is-dispatchable (contains? (methods attribute.hierarchy/description)
                                   [tag k])
        dispatch-tag (if is-dispatchable tag :default)
        active (and (= (:type clicked-element) :handle)
                    (= (:key clicked-element) key))]
    [:> HoverCard/Root
     [:> HoverCard/Trigger
      {:as-child true}
      [:div.flex.items-center.overflow-hidden
       [:label.form-element.w-28.truncate.flex-1.rtl:text-left!.py-0
        {:for (name k)
         :dir "ltr"
         :class ["leading-[27px]" (when active "text-foreground-hovered")]}
        k]]]
     [:> HoverCard/Portal
      [:> HoverCard/Content
       {:side "left"
        :class "popover-content"
        :align "start"
        :on-escape-key-down #(.stopPropagation %)}
       [:div.p-5
        [:h2.mb-4.text-lg.font-mono.text-foreground-hovered k]
        (when (get-method attribute.hierarchy/description [dispatch-tag k])
          [:p
           (i18n.views/t (attribute.hierarchy/description dispatch-tag k))])
        (when (utils.attribute/compatibility tag k)
          [:<>
           (when property
             (into [:<>]
                   (map (partial feature property))
                   features))
           [caniusethis {:tag tag
                         :attr k}]])]
       [views/hovercard-arrow]]]]))

(defn row
  [k v locked? tag]
  (let [initial (utils.attribute/initial-memo tag k)
        dispatchable? (contains? (methods attribute.hierarchy/form-element)
                                 [tag k])
        dispatch-tag (if dispatchable? tag :default)]
    [:<>
     [title tag k]
     [:div.flex.flex-1
      [attribute.hierarchy/form-element dispatch-tag k v
       {:disabled locked?
        :default-value initial
        :placeholder initial}]]]))

(defn tag-info
  [tag]
  (let [properties (element.hierarchy/properties tag)]
    [:div
     [:> HoverCard/Root
      [:> HoverCard/Trigger {:as-child true}
       [:div.flex.items-center
        [views/icon-button "info"
         {:title (i18n.views/t [::mdn-info "MDN Info"])
          :class "hover:bg-transparent w-auto h-auto"}]]]
      [:> HoverCard/Portal
       [:> HoverCard/Content
        {:sideOffset 5
         :class "popover-content"
         :align "end"
         :on-escape-key-down #(.stopPropagation %)}
        [:div.p-5
         [:h2.mb-4.text-lg.font-mono.text-foreground-hovered
          (str "<" (name tag) ">")]
         (when-let [description (:description properties)]
           [:p (i18n.views/t description)])
         [caniusethis {:tag tag}]
         (when-let [url (:url properties)]
           [:div.flex [info-button url]])]
        [views/hovercard-arrow]]]]]))

(defn form
  []
  (let [selected-elements @(rf/subscribe [::element.subs/selected])
        selected-tags @(rf/subscribe [::element.subs/selected-tags])
        selected-attrs @(rf/subscribe [::element.subs/selected-attrs])
        selected-locked? @(rf/subscribe [::element.subs/selected-locked?])
        tool-state @(rf/subscribe [::tool.subs/state])
        tool-cached-state @(rf/subscribe [::tool.subs/cached-state])
        tag (first selected-tags)
        multitag? (next selected-tags)
        locked? (or selected-locked?
                    (not= tool-state :idle)
                    (and tool-cached-state (not= tool-cached-state :idle)))]
    (when-first [el selected-elements]
      [:div
       [:div.flex.bg-primary.py-5.px-4.gap-1.items-center
        [:h1.flex-1.text-lg
         (if-not (next selected-elements)
           (let [el-label (:label el)
                 properties (element.hierarchy/properties tag)]
             (if (empty? el-label)
               (or (some-> properties :label i18n.views/t)
                   (string/capitalize (name tag)))
               el-label))
           (i18n.views/t [::attributes-title "%1 %2 elements"]
                         [(count selected-elements)
                          (when-not multitag?
                            (name tag))]))]
        (when-not multitag?
          [tag-info tag])]
       (when (seq selected-attrs)
         [:div.grid.grid-cols-2.grid-flow-row.my-px.w-full.gap-px
          {:style {:grid-template-columns "minmax(120px, 120px) 1fr"}}
          (for [[k v] selected-attrs]
            ^{:key k}
            [row k v locked? tag])])])))

(defmethod tool.hierarchy/right-panel :default [] [form])
