(ns renderer.element.subs
  (:require
   ["js-beautify" :as js-beautify]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.handlers :as element.handlers]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.element :as utils.element]))

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
 ::some-selected-tag?
 :<- [::selected-tags]
 :=> contains?)

(rf/reg-sub
 ::some-selected?
 :<- [::selected]
 :-> (comp boolean seq))

(rf/reg-sub
 ::every-selected-locked?
 :<- [::selected]
 :-> (partial every? :locked))

(rf/reg-sub
 ::multiple-selected?
 :<- [::selected]
 :-> (comp boolean seq rest))

(rf/reg-sub
 ::edit-attributes
 :<- [::selected]
 :-> utils.element/edit-attributes)

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

(rf/reg-sub
 ::handle-selected?
 :<- [::document.subs/elements]
 (fn [elements [_ el-id handle-id]]
   (-> (get-in elements [el-id :selected-handles])
       (contains? handle-id))))
