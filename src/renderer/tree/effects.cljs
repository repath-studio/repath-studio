(ns renderer.tree.effects
  (:require
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.events]))

(defn query-by-id!
  [id]
  (.querySelector js/document (str "#tree-sidebar [data-id='" id "']")))

(defn get-list-elements!
  []
  (->> (.querySelectorAll js/document "#tree-sidebar .list-item-button")
       (.from js/Array)))

(rf/reg-fx
 ::focus
 (fn []
   (some-> (.getElementById js/document "tree-sidebar")
           (.focus))))

(rf/reg-fx
 ::focus-first
 (fn []
   (some-> (get-list-elements!)
           (first)
           (.focus))))

(rf/reg-fx
 ::focus-last
 (fn []
   (some-> (get-list-elements!)
           (last)
           (.focus))))

(rf/reg-fx
 ::focus-next
 (fn [[id direction]]
   (let [list-elements (get-list-elements!)
         current-el (query-by-id! id)
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
 (fn [[last-focused-id id]]
   (let [list-elements (get-list-elements!)
         clicked-el (query-by-id! id)
         last-focus-el (query-by-id! last-focused-id)
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
