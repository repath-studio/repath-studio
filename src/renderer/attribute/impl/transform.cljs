(ns renderer.attribute.impl.transform
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/transform"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.i18n.views :as i18n.views]
   [renderer.utils.key :as utils.key]
   [renderer.utils.transform :as utils.transform]
   [renderer.views :as views]))

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :transform]
  []
  [::description
   "The transform attribute defines a list of transform definitions that are
    applied to an element and the element's children."])

(defmethod attribute.hierarchy/update-attr [::element.hierarchy/element
                                            :transform]
  [el k transform]
  (let [el (update-in el [:attrs k] utils.transform/compose-attr transform)]
    (cond-> el
      (nil? (-> el :attrs k))
      (update :attrs dissoc k))))

(def ops
  [[:translate {:labels ["x" "y"]
                :defaults [0 0]}]
   [:rotate {:labels ["angle" "cx" "cy"]
             :defaults [0 0 0]}]
   [:scale {:labels ["sx" "sy"]
            :defaults [1 1]}]
   [:skew-x {:labels ["angle"]
             :defaults [0]
             :row :skew}]
   [:skew-y {:labels ["angle"]
             :defaults [0]
             :row :skew}]
   [:matrix {:labels ["a" "b" "c" "d" "e" "f"]
             :defaults utils.transform/identity-transform}]])

(def field-labels (into {} (map (juxt first (comp :labels second)) ops)))

(def op-defaults (into {} (map (juxt first (comp :defaults second)) ops)))

(def op-order
  (mapv first ops))

(defn op-rows
  []
  (reduce (fn [acc [op-type {:keys [row labels]}]]
            (let [label (or row (keyword op-type))
                  inputs (mapv (partial vector op-type) (range (count labels)))]
              (if (and (seq acc)
                       (= (first (last acc)) label))
                (assoc (pop acc) (dec (count acc))
                       [label (into (peek (last acc)) inputs)])
                (conj acc [label inputs]))))
          [] ops))

(defn op-args-map
  [v]
  (reduce (fn [acc [op-type & args]]
            (assoc acc op-type (vec args)))
          {} (utils.transform/parse v)))

(defn op-args
  ([v op-type]
   (or (get (op-args-map v) op-type)
       (op-defaults op-type)))
  ([v op-type i]
   (nth (op-args v op-type) i)))

(defn transform-string
  [ops-map]
  (->> op-order
       (mapv (fn [op-type] (vec (cons op-type (or (get ops-map op-type)
                                                  (op-defaults op-type))))))
       (utils.transform/ops->string)))

(defn set-op-field
  [e {:keys [v op-type i]}]
  (let [new-v (utils.transform/->number (.. e -target -value))]
    (if (nil? new-v)
      (set! (.. e -target -value)
            (if (some? (get (op-args-map v) op-type))
              (op-args v op-type i)
              ""))
      (let [ops-map (op-args-map v)
            args (assoc (op-args v op-type) i new-v)]
        (rf/dispatch [::element.events/set-attr :transform
                      (transform-string (assoc ops-map op-type args))])))))

(defn op-input
  [v op-type i]
  (let [args (get (op-args-map v) op-type)
        value (op-args v op-type i)
        attrs {:v v
               :op-type op-type
               :i i}
        label (nth (field-labels op-type) i)]
    [:input.form-element.min-w-0.flex-1
     {:key (str op-type i (if (some? args) value :unset))
      :dir "ltr"
      :enter-key-hint "done"
      :aria-label label
      :title label
      :default-value (when (some? args) value)
      :placeholder (when (nil? args) value)
      :on-blur #(set-op-field % attrs)
      :on-pointer-up attribute.views/pointer-up-handler
      :on-key-down #(utils.key/down-handler % value set-op-field attrs)}]))

(defn op-row
  [v label inputs]
  [:div.grid.grid-flow-col.gap-px.items-center
   {:dir "ltr"
    :style {:grid-template-columns "minmax(5rem, auto) 1fr"}}
   [:span.form-element label]
   [:div.flex.gap-px
    (for [[op-type i] inputs]
      ^{:key (str op-type i)}
      [op-input v op-type i])]])

(defmethod attribute.hierarchy/form-element [::element.hierarchy/element
                                             :transform]
  [_ k v attrs]
  [:div.flex.gap-px.w-full
   [attribute.views/form-input k v attrs]
   [:> Popover/Root {:modal true}
    [:> Popover/Trigger
     [views/icon-button "pencil"
      {:class "form-control-button"
       :disabled (:disabled attrs)
       :title (i18n.views/t [::edit "Edit transform"])}]]
    [:> Popover/Portal
     [:> Popover/Content
      {:sideOffset 5
       :class "popover-content bg-secondary!"
       :align "end"
       :on-escape-key-down #(.stopPropagation %)}
      [:div.flex.flex-col.gap-px
       (for [[label inputs] (op-rows)]
         ^{:key label}
         [op-row v label inputs])]
      [views/popover-arrow]]]]])
