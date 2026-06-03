(ns renderer.tree.effects
  (:require
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.events]))

(def item-class "list-item-button")

(defn query-by-id
  [id tree-ref]
  (some-> tree-ref
          (.-current)
          (.querySelector (str "[data-id='" id "']"))))

(defn get-list-elements
  [tree-ref]
  (some-> tree-ref
          (.-current)
          (.querySelectorAll (str "." item-class))
          (->> (.from js/Array))))

(rf/reg-fx
 ::focus
 (fn [tree-ref]
   (some->> tree-ref
            (.-current)
            (.focus))))

(rf/reg-fx
 ::focus-first
 (fn [tree-ref]
   (some-> tree-ref
           (get-list-elements)
           (first)
           (.focus))))

(rf/reg-fx
 ::focus-last
 (fn [tree-ref]
   (some-> tree-ref
           (get-list-elements)
           (last)
           (.focus))))

(rf/reg-fx
 ::focus-next
 (fn [[id direction tree-ref]]
   (let [list-elements (get-list-elements tree-ref)
         current-el (query-by-id id tree-ref)
         index (.indexOf list-elements current-el)
         max-index (dec (count list-elements))
         updated-index (case direction
                         :up
                         (if (zero? index)
                           max-index
                           (dec index))

                         :down
                         (if (< index max-index)
                           (inc index)
                           0))]
     (some-> (get list-elements updated-index)
             (.focus)))))

(rf/reg-fx
 ::select-range
 (fn [[last-focused-id id tree-ref]]
   (let [list-elements (get-list-elements tree-ref)
         clicked-el (query-by-id id tree-ref)
         last-focus-el (query-by-id last-focused-id tree-ref)
         clicked-index (.indexOf list-elements clicked-el)
         focused-index (.indexOf list-elements last-focus-el)]
     (when-not (neg? focused-index)
       (let [index-range (apply range (if (< clicked-index focused-index)
                                        [clicked-index (inc focused-index)]
                                        [focused-index (inc clicked-index)]))
             ids (mapv #(-> (get list-elements %)
                            (.getAttribute "data-id")
                            (uuid)) index-range)]
         (rf/dispatch [::element.events/select-ids ids]))))))
