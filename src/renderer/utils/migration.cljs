(ns renderer.utils.migration
  (:require
   [clojure.set :as set]
   [clojure.string :as string]
   [renderer.utils.element :as utils.element]
   [renderer.utils.map :as utils.map]))

(def migrations
  [[[0 3 0] (fn [document]
              (-> document
                  (set/rename-keys {:key :id})
                  (update :elements
                          update-vals
                          #(-> %
                               (set/rename-keys {:key :id})
                               (utils.map/remove-nils)))))]

   [[0 4 0] (fn [document]
              (let [key->uuid (comp uuid name)]
                (cond-> document
                  (:id document)
                  (update :id key->uuid)

                  (:save document)
                  (update :save key->uuid)

                  :always
                  (-> (update :elements update-keys key->uuid)
                      (update :elements update-vals
                              #(cond-> %
                                 :always
                                 (-> (update :id key->uuid)
                                     (update :children (fn [ks]
                                                         (mapv key->uuid ks))))

                                 (:parent %)
                                 (update :parent key->uuid)))))))]

   [[0 4 4] (fn [document]
              (update document :elements update-vals
                      (fn [el]
                        (update-keys el #(case %
                                           :visible? :visible
                                           :selected? :selected
                                           :locked? :locked
                                           %)))))]

   [[0 4 5] (fn [document]
              (update document :elements update-vals
                      (fn [el]
                        (cond-> el
                          (= (:tag el) :brush)
                          (update-in [:attrs :points]
                                     #(string/join " " (flatten %)))

                          :always
                          (utils.element/normalize-attrs)))))]

   [[0 4 6] (fn [document]
              (update document :elements update-vals
                      (fn [el]
                        (update-keys el #(if (= :bounds %) :bbox %)))))]

   [[0 4 10] (fn [document]
               (dissoc document :save))]

   [[0 4 17] (fn [document]
               (update document :elements update-vals
                       (fn [el]
                         (cond-> el
                           (= (:tag el) :brush)
                           (update :attrs (fn [attrs]
                                            (update-keys attrs
                                                         #(if (= :stroke %)
                                                            :fill
                                                            %))))))))]])
