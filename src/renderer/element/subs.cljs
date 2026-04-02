(ns renderer.element.subs
  (:require
   ["js-beautify" :as js-beautify]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.element :as utils.element]
   [renderer.utils.map :as utils.map]))

(rf/reg-sub
 ::root
 element.handlers/root)

(rf/reg-sub
 ::root-children
 :<- [::document.subs/elements]
 :<- [::root]
 (fn [[elements root] _]
   (mapv elements (:children root))))

(rf/reg-sub
 ::entity
 :<- [::document.subs/elements]
 (fn [elements [_ id]]
   (get elements id)))

(rf/reg-sub
 ::entities
 :<- [::document.subs/elements]
 vals)

(rf/reg-sub
 ::xml
 :<- [::root-children]
 (fn [root-children _]
   (-> (utils.element/->string root-children)
       (js-beautify/html #js {:indent_size 2}))))

(rf/reg-sub
 ::filter-visible
 :<- [::document.subs/elements]
 (fn [elements [_ ks]]
   (filter :visible (mapv #(get elements %) ks))))

(rf/reg-sub
 ::selected
 :<- [::entities]
 (fn [entities _]
   (filter :selected entities)))

(rf/reg-sub
 ::hovered
 :<- [::document.subs/elements]
 :<- [::document.subs/hovered-ids]
 (fn [[elements hovered-ids] _]
   (vals (select-keys elements hovered-ids))))

(rf/reg-sub
 ::selected-tags
 :<- [::selected]
 (fn [selected-elements _]
   (->> selected-elements (map :tag) set)))

(rf/reg-sub
 ::has-selected-tag?
 :<- [::selected-tags]
 (fn [selected-tags [_ tag]]
   (contains? selected-tags tag)))

(rf/reg-sub
 ::some-selected?
 :<- [::selected]
 (comp boolean seq))

(rf/reg-sub
 ::selected-locked?
 :<- [::selected]
 (fn [selected-elements _]
   (every? :locked selected-elements)))

(rf/reg-sub
 ::multiple-selected?
 :<- [::selected]
 (fn [selected-elements _]
   (boolean (seq (rest selected-elements)))))

(rf/reg-sub
 ::selected-attrs
 :<- [::selected]
 :<- [::multiple-selected?]
 (fn [[selected-elements multiple-selected?] _]
   (when (seq selected-elements)
     (let [attrs (->> selected-elements
                      (map utils.element/attributes)
                      (apply utils.map/merge-common-with
                             (fn [v1 v2] (when (= v1 v2) v1))))
           attrs (if multiple-selected?
                   (dissoc attrs :id)
                   (let [el (first selected-elements)
                         props (utils.element/properties el)]
                     (->> (utils.element/attributes el)
                          (sort-by (fn [[id _]]
                                     (-> props :attrs (.indexOf id)))))))]
       (sort-by (fn [[id _]] (.indexOf utils.attribute/order id)) attrs)))))

(rf/reg-sub
 ::bbox
 :<- [::selected]
 (fn [selected-elements _]
   (utils.element/united-bbox selected-elements)))

(rf/reg-sub
 ::area
 :<- [::selected]
 (fn [selected-elements _]
   (reduce #(+ %1 (element.hierarchy/area %2)) 0 selected-elements)))

(rf/reg-sub
 ::ancestor-ids
 (fn [db _]
   (element.handlers/ancestor-ids db)))

(rf/reg-sub
 ::font-styles
 :<- [::selected]
 :<- [::app.subs/system-fonts]
 (fn [[selected-elements system-fonts] _]
   (into #{}
         (comp (keep #(-> % :attrs :font-family))
               (mapcat #(-> (get system-fonts %) keys)))
         selected-elements)))

(rf/reg-sub
 ::font-weights
 :<- [::font-styles]
 (fn [font-styles _]
   (into #{}
         (mapcat (fn [style]
                   (->> utils.attribute/weight-name-mapping
                        (filter (fn [[_k v]]
                                  (some #(string/includes? style %) v)))
                        (map first))))
         font-styles)))

(rf/reg-sub
 ::every-top-level?
 :<- [::root]
 :<- [::ancestor-ids]
 (fn [[root ancestor-ids] _]
   (->> (:id root)
        (disj (set ancestor-ids))
        (empty?))))

(rf/reg-sub
 ::not-every-top-level?
 :<- [::every-top-level?]
 not)
