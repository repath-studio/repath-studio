(ns renderer.element.subs
  (:require
   ["js-beautify" :as js-beautify]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.handlers :as element.handlers]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.element :as utils.element]
   [renderer.utils.map :as utils.map]))

(rf/reg-sub
 ::root
 :-> element.handlers/root)

(rf/reg-sub
 ::root-children
 :<- [::document.subs/elements]
 :<- [::root]
 (fn [[elements root] _]
   (->> (:children root)
        (mapv elements)
        (filterv (complement utils.element/virtual?)))))

(rf/reg-sub
 ::entity
 :<- [::document.subs/elements]
 :=> get)

(rf/reg-sub
 ::entities
 :<- [::document.subs/elements]
 :-> vals)

(rf/reg-sub
 ::xml
 :<- [::root-children]
 (fn [root-children _]
   (-> root-children
       (utils.element/->string)
       (js-beautify/html #js {:indent_size 2}))))

(rf/reg-sub
 ::filter-visible
 :<- [::document.subs/elements]
 (fn [elements [_ ks]]
   (filter :visible (mapv #(get elements %) ks))))

(rf/reg-sub
 ::selected
 :<- [::entities]
 :-> (partial filter :selected))

(rf/reg-sub
 ::hovered
 :<- [::document.subs/elements]
 :<- [::document.subs/hovered-ids]
 (fn [[elements hovered-ids] _]
   (vals (select-keys elements hovered-ids))))

(rf/reg-sub
 ::hovered?
 :<- [::document.subs/hovered-ids]
 :=> contains?)

(rf/reg-sub
 ::selected-tags
 :<- [::selected]
 :-> (comp set (partial map :tag)))

(rf/reg-sub
 ::has-selected-tag?
 :<- [::selected-tags]
 :=> contains?)

(rf/reg-sub
 ::some-selected?
 :<- [::selected]
 :-> (comp boolean seq))

(rf/reg-sub
 ::selected-locked?
 :<- [::selected]
 :-> (partial every? :locked))

(rf/reg-sub
 ::multiple-selected?
 :<- [::selected]
 :-> (comp boolean seq rest))

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
 :-> utils.element/united-bbox)

(rf/reg-sub
 ::area
 :<- [::selected]
 :-> utils.element/area)

(rf/reg-sub
 ::ancestor-ids
 :-> element.handlers/ancestor-ids)

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
 :-> not)
