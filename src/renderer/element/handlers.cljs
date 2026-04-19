(ns renderer.element.handlers
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.set :as set]
   [clojure.string :as string]
   [clojure.zip :as zip]
   [hickory.core :as hickory]
   [hickory.zip]
   [malli.core :as m]
   [malli.error :as m.error]
   [renderer.app.db :refer [App]]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.db :refer [BBox BooleanOperation PathManipulation Vec2]]
   [renderer.element.db
    :as element.db
    :refer [ElementAttrs Element ElementId ElementTag AnimationTag Direction]]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.tool.db :refer [HandleId]]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.extra :refer [partial-right]]
   [renderer.utils.path :as utils.path]
   [renderer.utils.vec :as utils.vec]))

(m/=> path [:function
            [:-> App vector?]
            [:-> App ElementId vector?]
            [:-> App ElementId keyword? vector?]
            [:-> App ElementId keyword? [:* any?] vector?]])
(defn path
  ([db]
   [:documents (:active-document db) :elements])
  ([db id]
   (conj (path db) id))
  ([db id prop]
   (conj (path db) id prop))
  ([db id prop & more]
   (apply conj (path db) id prop more)))

(m/=> entities [:function
                [:-> App [:maybe [:sequential Element]]]
                [:-> App [:maybe [:vector ElementId]] [:maybe
                                                       [:sequential Element]]]])
(defn entities
  ([db]
   (vals (get-in db (path db))))
  ([db ids]
   (vals (select-keys (get-in db (path db)) ids))))

(m/=> entity [:-> App [:maybe ElementId] [:maybe Element]])
(defn entity
  [db id]
  (get-in db (path db id)))

(m/=> root [:-> App Element])
(defn root
  [db]
  (some #(when (utils.element/root? %) %) (entities db)))

(m/=> locked? [:-> App ElementId boolean?])
(defn locked?
  [db id]
  (-> db (entity id) :locked boolean))

(m/=> selected [:function
                [:-> fn?]
                [:-> App [:sequential Element]]])
(defn selected
  ([]
   (filter :selected))
  ([db]
   (into [] (selected) (entities db))))

(m/=> visible [:function
               [:-> fn?]
               [:-> App [:sequential Element]]])
(defn visible
  ([]
   (filter :visible))
  ([db]
   (into [] (visible) (entities db))))

(m/=> ratio-locked? [:-> App boolean?])
(defn ratio-locked?
  [db]
  (every? utils.element/ratio-locked? (selected db)))

(m/=> selected-ids [:function
                    [:-> fn?]
                    [:-> App [:set ElementId]]])
(defn selected-ids
  ([]
   (comp (selected) (map :id)))
  ([db]
   (into #{} (selected-ids) (entities db))))

(m/=> children-ids [:-> App ElementId [:vector ElementId]])
(defn children-ids
  [db id]
  (:children (entity db id)))

(m/=> parent-ids [:function
                  [:-> fn?]
                  [:-> App [:set ElementId]]])
(defn parent-ids
  ([]
   (comp (selected) (keep :parent)))
  ([db]
   (into #{} (parent-ids) (entities db))))

(m/=> parent [:function
              [:-> App [:maybe Element]]
              [:-> App [:maybe ElementId] [:maybe Element]]])
(defn parent
  ([db]
   (let [ids (parent-ids db)]
     (if (= (count ids) 1)
       (entity db (first ids))
       (root db))))
  ([db id]
   (some->> (entity db id)
            :parent
            (entity db))))

(m/=> parent-container [:-> App ElementId [:maybe Element]])
(defn parent-container
  [db id]
  (when-let [parent-el (parent db id)]
    (if (utils.element/container? parent-el)
      parent-el
      (recur db (:id parent-el)))))

(m/=> parent-offset [:-> App Vec2])
(defn parent-offset
  [db]
  (or (some->> (selected-ids db)
               (first)
               (parent-container db)
               (element.hierarchy/bbox)
               (take 2)
               (into []))
      [0 0]))

(m/=> adjusted-bbox [:-> App ElementId [:maybe BBox]])
(defn adjusted-bbox
  [db id]
  (loop [container (parent-container db id)
         bbox (element.hierarchy/bbox (entity db id))]
    (if-not (and container bbox)
      bbox
      (let [[offset-x offset-y _ _] (element.hierarchy/bbox container)
            [min-x min-y max-x max-y] bbox]
        (recur
         (parent-container db (:id container))
         [(+ min-x offset-x)
          (+ min-y offset-y)
          (+ max-x offset-x)
          (+ max-y offset-y)])))))

(m/=> refresh-bbox [:-> App ElementId App])
(defn refresh-bbox
  [db id]
  (let [el (entity db id)
        children (children-ids db id)
        bbox (if (= (:tag el) :g)
               (let [b (map #(adjusted-bbox db %) children)]
                 (when (seq b) (apply utils.bounds/union b)))
               (adjusted-bbox db id))]
    (if (or (not bbox) (utils.element/root? el))
      db
      (-> (reduce refresh-bbox db children)
          (update-in (path db id) assoc :bbox bbox)))))

(m/=> update-el [:function
                 [:-> App ElementId ifn? App]
                 [:-> App ElementId ifn? [:* any?] App]])
(defn update-el
  ([db id f]
   (if (locked? db id)
     db
     (-> (update-in db (path db id) f)
         (refresh-bbox id))))
  ([db id f arg]
   (if (locked? db id)
     db
     (-> (update-in db (path db id) f arg)
         (refresh-bbox id))))
  ([db id f arg & more]
   (if (locked? db id)
     db
     (-> (apply update-in db (path db id) f arg more)
         (refresh-bbox id)))))

(m/=> update-selected [:function
                       [:-> App ifn? App]
                       [:-> App ifn? [:* any?] App]])
(defn update-selected
  ([db f]
   (reduce (fn [db id] (update-el db id f)) db (selected-ids db)))
  ([db f arg]
   (reduce (fn [db id] (update-el db id f arg)) db (selected-ids db)))
  ([db f arg & more]
   (reduce (fn [db id] (apply update-el db id f arg more))
           db
           (selected-ids db))))

(m/=> siblings-selected? [:-> App [:maybe boolean?]])
(defn siblings-selected?
  [db]
  (let [ids (parent-ids db)]
    (and (seq ids)
         (empty? (rest ids))
         (= (count (selected db))
            (count (children-ids db (first ids)))))))

(m/=> siblings [:function
                [:-> App [:maybe [:vector ElementId]]]
                [:-> App ElementId [:maybe [:vector ElementId]]]])
(defn siblings
  ([db]
   (:children (parent db)))
  ([db id]
   (:children (parent db id))))

(m/=> root-children [:-> App [:maybe [:sequential Element]]])
(defn root-children
  [db]
  (entities db (:children (root db))))

(m/=> root-svgs [:-> App [:sequential Element]])
(defn root-svgs
  [db]
  (->> (root-children db)
       (filter utils.element/svg?)))

(m/=> ancestor-ids [:function
                    [:-> App [:sequential ElementId]]
                    [:-> App ElementId [:sequential ElementId]]])
(defn ancestor-ids
  ([db]
   (reduce #(concat %1 (ancestor-ids db %2)) [] (selected-ids db)))
  ([db id]
   (loop [parent-id (:parent (entity db id))
          ids []]
     (if parent-id
       (recur
        (:parent (entity db parent-id))
        (conj ids parent-id))
       ids))))

(m/=> children-index [:-> App ElementId [:maybe int?]])
(defn children-index
  "Returns the index of an element on its parent children vector."
  [db id]
  (some-> (siblings db id)
          (.indexOf id)))

(m/=> index-path [:-> App ElementId [:vector int?]])
(defn index-path
  "Returns a sequence that represents the index path of an element.
   For example, the seventh element of the second svg on the canvas will
   return [2 7]. This is useful when we need to figure out the global index
   of nested elements."
  [db id]
  (let [ancestor-els (reverse (ancestor-ids db id))]
    (conj (into [] (keep (partial children-index db)) ancestor-els)
          (children-index db id))))

(m/=> descendant-ids [:function
                      [:-> App [:set ElementId]]
                      [:-> App ElementId [:set ElementId]]])
(defn descendant-ids
  ([db]
   (into #{} (comp (selected-ids)
                   (mapcat #(descendant-ids db %))) (entities db)))
  ([db id]
   (loop [children-set (set (children-ids db id))
          child-keys #{}]
     (if (seq children-set)
       (recur
        (reduce #(set/union %1 (set (children-ids db %2))) #{} children-set)
        (set/union child-keys children-set))
       child-keys))))

(m/=> top-ancestor-ids [:-> App [:set ElementId]])
(defn top-ancestor-ids
  [db]
  (set/difference (selected-ids db) (descendant-ids db)))

(m/=> selected-with-descendant-ids [:-> App [:set ElementId]])
(defn selected-with-descendant-ids
  [db]
  (set/union (selected-ids db) (descendant-ids db)))

(m/=> non-selected-ids [:-> App [:maybe [:set ElementId]]])
(defn non-selected-ids
  [db]
  (set/difference (->> (path db) (get-in db) keys (into #{}))
                  (selected-with-descendant-ids db)))

(m/=> non-selected-visible [:-> App [:sequential Element]])
(defn non-selected-visible
  [db]
  (sequence (comp (filter (complement :selected))
                  (visible))
            (entities db)))

(m/=> top-selected-ancestors [:-> App [:sequential Element]])
(defn top-selected-ancestors
  [db]
  (entities db (-> (top-ancestor-ids db)
                   (disj (:id (root db)))
                   (vec))))

(m/=> update-prop [:-> App ElementId ifn? [:* any?] App])
(defn update-prop
  ([db id k f]
   (update-in db (path db id k) f))
  ([db id k f arg]
   (update-in db (path db id k) f arg))
  ([db id k f arg & more]
   (apply update-in db (path db id k) f arg more)))

(m/=> assoc-prop [:function
                  [:-> App keyword? any? App]
                  [:-> App ElementId keyword? any? App]])
(defn assoc-prop
  ([db k v]
   (reduce (partial-right assoc-prop k v) db (selected-ids db)))
  ([db id k v]
   (if (string/blank? v)
     (update-in db (path db id) dissoc k)
     (assoc-in db (path db id k) v))))

(m/=> dissoc-attr [:function
                   [:-> App keyword? App]
                   [:-> App ElementId keyword? App]])
(defn dissoc-attr
  ([db k]
   (reduce (partial-right dissoc-attr k) db (selected-ids db)))
  ([db id k]
   (cond-> db
     (not (locked? db id))
     (-> (update-prop id :attrs dissoc k)
         (refresh-bbox id)))))

(m/=> assoc-attr [:function
                  [:-> App keyword? string? App]
                  [:-> App ElementId keyword? string? App]])
(defn assoc-attr
  ([db k v]
   (reduce (partial-right assoc-attr k v) db (selected-ids db)))
  ([db id k v]
   (cond-> db
     (not (locked? db id))
     (-> (assoc-in (path db id :attrs k) v)
         (refresh-bbox id)))))

(m/=> set-attr [:function
                [:-> App keyword? any? App]
                [:-> App ElementId keyword? any? App]])
(defn set-attr
  ([db k v]
   (reduce (partial-right set-attr k v) db (selected-ids db)))
  ([db id k v]
   (if (and (not (locked? db id))
            (utils.element/supported-attr? (entity db id) k))
     (if (string/blank? v)
       (dissoc-attr db id k)
       (assoc-attr db id k (string/trim (str v))))
     db)))

(m/=> update-attr [:-> App ElementId keyword? ifn? [:* any?] App])
(defn update-attr
  ([db id k f]
   (cond-> db
     (utils.element/supported-attr? (entity db id) k)
     (update-el id attribute.hierarchy/update-attr k f)))
  ([db id k f arg]
   (cond-> db
     (utils.element/supported-attr? (entity db id) k)
     (update-el id attribute.hierarchy/update-attr k f arg)))
  ([db id k f arg & more]
   (if (utils.element/supported-attr? (entity db id) k)
     (apply update-el db id attribute.hierarchy/update-attr k f arg more)
     db)))

(m/=> deselect [:function
                [:-> App App]
                [:-> App ElementId App]])
(defn deselect
  ([db]
   (transduce (selected-ids) (completing deselect) db (entities db)))
  ([db id]
   (assoc-prop db id :selected false)))

(m/=> collapse [:-> App ElementId App])
(defn collapse
  [db id]
  (update-in db [:documents (:active-document db) :collapsed-ids] conj id))

(m/=> collapse-all [:-> App App])
(defn collapse-all
  [db]
  (transduce (map :id) (completing collapse) db (entities db)))

(m/=> expand [:-> App ElementId App])
(defn expand
  [db id]
  (update-in db [:documents (:active-document db) :collapsed-ids] disj id))

(m/=> expand-ancestors [:-> App ElementId App])
(defn expand-ancestors
  [db id]
  (reduce expand db (ancestor-ids db id)))

(m/=> select [:-> App ElementId App])
(defn select
  [db id]
  (-> db
      (expand-ancestors id)
      (assoc-prop id :selected true)))

(m/=> toggle-selection [:-> App [:or ElementId HandleId] boolean? App])
(defn toggle-selection
  [db id additive]
  (if-not (m/validate ElementId id)
    db
    (let [root-id (:id (root db))]
      (if (entity db id)
        (if (and additive
                 (not= id root-id)
                 (not (contains? (selected-ids db) root-id)))
          (update-prop db id :selected not)
          (-> db deselect (select id)))
        (deselect db)))))

(m/=> select-all [:-> App App])
(defn select-all
  [db]
  (reduce select db (if (siblings-selected? db)
                      (children-ids db (:id (parent db (:id (parent db)))))
                      (siblings db))))

(m/=> selected-tags [:-> App [:set ElementTag]])
(defn selected-tags
  [db]
  (into #{} (comp (selected)
                  (map :tag)) (entities db)))

(m/=> filter-by-tag [:-> App ElementTag [:sequential Element]])
(defn filter-by-tag
  ([tag]
   (filter #(= tag (:tag %))))
  ([db tag]
   (into [] (filter-by-tag tag) (entities db))))

(m/=> filter-selected-by-tag [:-> App ElementTag [:sequential Element]])
(defn filter-selected-by-tag
  ([tag]
   (comp (selected) (filter-by-tag tag)))
  ([db tag]
   (into [] (filter-selected-by-tag tag) (entities db))))

(m/=> select-same-tags [:-> App App])
(defn select-same-tags
  ([db]
   (let [tags (selected-tags db)]
     (transduce (comp (filter #(contains? tags (:tag %)))
                      (map :id))
                (completing select)
                db
                (entities db)))))

(m/=> sort-by-index-path [:-> App [:sequential Element] [:sequential Element]])
(defn sort-by-index-path
  [db els]
  (sort-by #(index-path db (:id %)) els))

(m/=> selected-sorted [:-> App [:sequential Element]])
(defn selected-sorted
  [db]
  (->> (selected db)
       (sort-by-index-path db)))

(m/=> top-selected-sorted [:-> App [:sequential Element]])
(defn top-selected-sorted
  [db]
  (->> (top-selected-ancestors db)
       (sort-by-index-path db)))

(m/=> selected-sorted-ids [:-> App [:vector ElementId]])
(defn selected-sorted-ids
  [db]
  (mapv :id (selected-sorted db)))

(m/=> top-selected-sorted-ids [:-> App [:vector ElementId]])
(defn top-selected-sorted-ids
  [db]
  (->> (top-selected-sorted db)
       (mapv :id)))

(m/=> invert-selection [:-> App App])
(defn invert-selection
  [db]
  (->> (entities db)
       (reduce (fn [db el]
                 (cond-> db
                   (not (contains? #{:svg :canvas} (:tag el)))
                   (update-prop (:id el) :selected not))) db)))

(m/=> hover [:-> App [:or ElementId HandleId] App])
(defn hover
  [db id]
  (update-in db [:documents (:active-document db) :hovered-ids] conj id))

(m/=> ignore [:-> App [:or ElementId HandleId] App])
(defn ignore
  [db id]
  (cond-> db
    (and (:active-document db) id)
    (update-in [:documents (:active-document db) :ignored-ids] conj id)))

(m/=> clear-hovered [:-> App App])
(defn clear-hovered
  [db]
  (cond-> db
    (:active-document db)
    (assoc-in [:documents (:active-document db) :hovered-ids] #{})))

(m/=> unignore [:-> App [:or ElementId HandleId] App])
(defn unignore
  [db id]
  (cond-> db
    (:active-document db)
    (update-in [:documents (:active-document db) :ignored-ids] disj id)))

(m/=> clear-ignored [:-> App App])
(defn clear-ignored
  [db]
  (cond-> db
    (:active-document db)
    (assoc-in [:documents (:active-document db) :ignored-ids] #{})))

(m/=> bbox [:-> App [:maybe BBox]])
(defn bbox
  [db]
  (utils.element/united-bbox (selected db)))

(m/=> copy [:-> App App])
(defn copy
  [db]
  (let [els (top-selected-sorted db)]
    (cond-> db
      (seq els)
      (assoc :clipboard {:elements els
                         :bbox (bbox db)}))))

(m/=> delete [:function
              [:-> App App]
              [:-> App ElementId App]])
(defn delete
  ([db]
   (reduce delete db (reverse (selected-sorted-ids db))))
  ([db id]
   (let [el (entity db id)
         db (if (utils.element/root? el) db (reduce delete db (:children el)))]
     (cond-> db
       (not (utils.element/root? el))
       (-> (update-prop (:parent el) :children
                        utils.vec/remove-nth (children-index db id))
           (update-in (path db) dissoc id)
           (expand id))))))

(m/=> update-index [:function
                    [:-> App ifn? App]
                    [:-> App ElementId ifn? App]])
(defn update-index
  ([db f]
   (reduce (partial-right update-index f) db (selected-sorted-ids db)))
  ([db id f]
   (let [sibling-count (count (siblings db id))
         i (children-index db id)
         new-index (f i sibling-count)]
     (cond-> db
       (<= 0 new-index (dec sibling-count))
       (update-prop (:id (parent db id)) :children
                    utils.vec/move i new-index)))))

(m/=> set-parent [:function
                  [:-> App ElementId App]
                  [:-> App ElementId ElementId App]
                  [:-> App ElementId ElementId int? App]])
(defn set-parent
  ([db parent-id]
   (reduce (partial-right set-parent parent-id) db (selected-sorted-ids db)))
  ([db id parent-id]
   (let [sibling-els (:children (entity db parent-id))
         last-index (count sibling-els)]
     (set-parent db id parent-id last-index)))
  ([db id parent-id i]
   (let [el (entity db id)]
     (cond-> db
       (and el (not= id parent-id) (not (locked? db id)))
       (-> (update-prop (:parent el) :children #(vec (remove #{id} %)))
           (update-prop parent-id :children utils.vec/add i id)
           (assoc-prop id :parent parent-id)
           (refresh-bbox id))))))

(m/=> hovered-svg [:-> App Element])
(defn hovered-svg
  [db]
  (let [svgs (reverse (root-svgs db))
        pointer-pos (:adjusted-pointer-pos db)]
    (or (some #(when (utils.bounds/contained-point? (:bbox %) pointer-pos) %)
              svgs)
        (root db))))

(m/=> translate [:function
                 [:-> App Vec2 App]
                 [:-> App ElementId Vec2 App]])
(defn translate
  "Moves elements by a given offset."
  ([db offset]
   (reduce (partial-right translate offset) db (top-ancestor-ids db)))
  ([db id offset]
   (update-el db id element.hierarchy/translate offset)))

(m/=> place [:function
             [:-> App Vec2 App]
             [:-> App ElementId Vec2 App]])
(defn place
  "Positions elements to a given global position."
  ([db position]
   (reduce (partial-right place position) db (top-ancestor-ids db)))
  ([db id position]
   (let [el (entity db id)
         center (utils.bounds/center (element.hierarchy/bbox el))
         offset (matrix/sub position center)]
     (update-el db id element.hierarchy/translate offset))))

(m/=> scale [:-> App Vec2 Vec2 boolean? App])
(defn scale
  [db ratio pivot-point recursive]
  (let [ids-to-scale (cond-> (selected-ids db)
                       recursive
                       (set/union (descendant-ids db)))]
    (reduce
     (fn [db id]
       (let [adjusted-pivot-point (some->> (entity db id)
                                           :bbox
                                           (take 2)
                                           (matrix/sub pivot-point))]
         (update-el db id element.hierarchy/scale ratio adjusted-pivot-point)))
     db
     ids-to-scale)))

(m/=> align [:function
             [:-> App Direction App]
             [:-> App ElementId Direction App]])
(defn align
  ([db direction]
   (reduce (partial-right align direction) db (selected-ids db)))
  ([db id direction]
   (let [el-bbox (:bbox (entity db id))
         center (utils.bounds/center el-bbox)
         parent-bbox (:bbox (parent db id))
         parent-center (utils.bounds/center parent-bbox)
         [cx cy] (matrix/sub parent-center center)
         delta-bbox (matrix/sub parent-bbox el-bbox)
         [min-x-delta min-y-delta max-x-delta max-y-delta] delta-bbox]
     (translate db id (case direction
                        :top [0 min-y-delta]
                        :center-vertical [0 cy]
                        :bottom [0 max-y-delta]
                        :left [min-x-delta 0]
                        :center-horizontal [cx 0]
                        :right [max-x-delta 0])))))

(m/=> stroke->path [:function
                    [:-> App App]
                    [:-> App ElementId App]])
(defn stroke->path
  ([db]
   (reduce stroke->path db (selected-ids db)))
  ([db id]
   (update-el db id utils.element/stroke->path)))

(m/=> overlapping-svg [:-> App BBox Element])
(defn overlapping-svg
  [db el-bbox]
  (let [svgs (reverse (root-svgs db))] ; Reverse to select top svgs first.
    (or (some #(when (utils.bounds/contained? el-bbox (:bbox %)) %) svgs)
        (some #(when (utils.bounds/intersect? el-bbox (:bbox %)) %) svgs)
        (root db))))

(m/=> assoc-parent-id [:-> App Element Element])
(defn assoc-parent-id
  [db el]
  (cond-> el
    (not (or (utils.element/root? el) (:parent el)))
    (assoc :parent (:id (if (utils.element/svg? el)
                          (root db)
                          (if-let [el-bbox (element.hierarchy/bbox el)]
                            (overlapping-svg db el-bbox)
                            (root db)))))))

(m/=> create [:-> App map? App])
(defn create
  [db el]
  (let [id (random-uuid) ; REVIEW: Hard to use a coeffect because of recursion.
        new-el (->> (assoc (utils.element/normalize el) :id id)
                    (assoc-parent-id db))
        child-els (-> (entities db (:children el))
                      (concat (:content el)))
        parent-el (entity db (:parent new-el))
        [min-x min-y] (some-> parent-el element.hierarchy/bbox)
        add-children (fn [db child-els]
                       (reduce #(cond-> %1
                                  (element.db/tag? (:tag %2))
                                  (create (assoc %2 :parent id)))
                               db child-els))]
    (if-not (element.db/valid? new-el)
      (throw (ex-info (str "Invalid element: "
                           (m.error/humanize (element.db/explain el)))
                      {:element new-el}))
      (let [is-translated (or (utils.element/svg? new-el)
                              (utils.element/root? new-el)
                              (:parent el))]
        (cond-> db
          :always
          (assoc-in (path db id) new-el)

          (:parent new-el)
          (-> (update-prop (:parent new-el) :children #(vec (conj % id)))
              (expand (:parent new-el)))

          (and (not is-translated) parent-el)
          (translate [(- min-x) (- min-y)])

          is-translated
          (refresh-bbox id)

          child-els
          (add-children child-els))))))

(m/=> create-default-canvas [:-> App [:maybe Vec2] App])
(defn create-default-canvas
  [db size]
  (cond-> db
    :always
    (create {:tag :canvas})

    size
    (create {:tag :svg
             :attrs {:width (first size)
                     :height (second size)}})))

(m/=> add [:-> App map? App])
(defn add
  [db el]
  (-> db
      (deselect)
      (create (assoc el :selected true))))

(m/=> swap [:-> App Element App])
(defn swap
  [db el]
  (-> db
      (assoc-in (path db (:id el)) el)
      (refresh-bbox (:id el))))

(m/=> boolean-operation [:-> App BooleanOperation App])
(defn boolean-operation
  [db operation]
  (let [selected-elements (top-selected-sorted db)
        attrs (-> selected-elements first :attrs)
        new-path (->> (rest selected-elements)
                      (reduce (fn [path-a el]
                                (let [path-b (-> el :attrs :d)]
                                  (utils.path/boolean-operation path-a
                                                                path-b
                                                                operation)))
                              (:d attrs)))]
    (cond-> db
      (seq new-path)
      (-> (delete)
          (add {:type :element
                :tag :path
                :parent (-> selected-elements first :parent)
                :attrs (merge attrs {:d new-path})})))))

(m/=> paste-in-place [:function
                      [:-> App App]
                      [:-> App Element App]])
(defn paste-in-place
  ([db]
   (reduce paste-in-place (deselect db) (-> db :clipboard :elements)))
  ([db el]
   (->> (selected-ids db)
        (reduce select (add db el)))))

(m/=> paste [:function
             [:-> App App]
             [:-> App Element Element App]])
(defn paste
  ([db]
   (let [parent-el (hovered-svg db)]
     (reduce (partial-right paste parent-el) (deselect db)
             (-> db :clipboard :elements))))
  ([db el parent-el]
   (let [center (utils.bounds/center (-> db :clipboard :bbox))
         el-center (utils.bounds/center (:bbox el))
         offset (matrix/sub el-center center)
         el (dissoc el :bbox)
         [s-x1 s-y1] (:bbox parent-el)
         pointer-pos (:adjusted-pointer-pos db)]
     (reduce select
             (cond-> db
               :always
               (-> (deselect)
                   (add (assoc el :parent (:id parent-el)))
                   (place (matrix/add pointer-pos offset)))

               (not= (:id (root db)) (:id parent-el))
               (translate [(- s-x1) (- s-y1)]))
             (selected-ids db)))))

(m/=> duplicate [:-> App App])
(defn duplicate
  [db]
  (reduce create (deselect db) (top-selected-sorted db)))

(m/=> animate [:function
               [:-> App AnimationTag App]
               [:-> App AnimationTag ElementAttrs App]
               [:-> App ElementId AnimationTag ElementAttrs App]])
(defn animate
  ([db tag]
   (animate db tag {}))
  ([db tag attrs]
   (reduce (partial-right animate tag attrs) (deselect db) (selected-ids db)))
  ([db id tag attrs]
   (reduce select (add db {:tag tag
                           :attrs attrs
                           :parent id}) (selected-ids db))))

(m/=> paste-styles [:function
                    [:-> App App]
                    [:-> App ElementId App]])
(defn paste-styles
  ([db]
   (reduce paste-styles db (selected-ids db)))
  ([db id]
   ;; TODO: Merge attributes from multiple selected elements.
   (if (= 1 (count (-> db :clipboard :elements)))
     (let [attrs (-> db :clipboard :elements first :attrs)
           style-attrs (disj utils.attribute/presentation :transform)]
       (reduce (fn [db attr]
                 (cond-> db
                   (attr attrs)
                   (update-attr id attr #(if % (-> attrs attr) disj))))
               db style-attrs)) db)))

(m/=> inherit-attrs [:-> App Element ElementId App])
(defn inherit-attrs
  [db source-el id]
  (reduce
   (fn [db attr]
     (let [source-attr (-> source-el :attrs attr)
           get-value (fn [v] (if (empty? (str v)) source-attr v))]
       (cond-> db
         source-attr
         (update-attr id attr get-value)))) db utils.attribute/presentation))

(m/=> group [:function
             [:-> App App]
             [:-> App [:sequential ElementId] App]])
(defn group
  ([db]
   (group db (top-selected-sorted-ids db)))
  ([db ids]
   (reduce (fn [db id] (set-parent db id (-> db selected-ids first)))
           (add db {:tag :g
                    :parent (:id (parent db))}) ids)))

(m/=> ungroup [:function
               [:-> App App]
               [:-> App ElementId App]])
(defn ungroup
  ([db]
   (reduce ungroup db (selected-ids db)))
  ([db id]
   (cond-> db
     (and (not (locked? db id)) (= (:tag (entity db id)) :g))
     (as-> db db
       (let [index (children-index db id)]
         (reduce
          (fn [db child-id]
            (-> db
                (set-parent child-id (:parent (entity db id)) index)
                ;; Group attributes are inherited by its children,
                ;; so we need to maintain the presentation attrs.
                (inherit-attrs (entity db id) child-id)
                (select child-id)))
          db (reverse (children-ids db id))))
       (delete db id)))))

(m/=> manipulate-path [:function
                       [:-> App PathManipulation App]
                       [:-> App ElementId PathManipulation App]])
(defn manipulate-path
  ([db action]
   (reduce (partial-right manipulate-path action) db (selected-ids db)))
  ([db id action]
   (cond-> db
     (= (:tag (entity db id)) :path)
     (update-attr id :d utils.path/manipulate action))))

(def SvgData [:map
              [:svg string?]
              [:label string?]
              [:position Vec2]])

(defn find-svg
  [zipper]
  (loop [loc zipper]
    (if (zip/end? loc)
      (zip/root loc)
      (if (= (:tag (zip/node loc)) :svg)
        (zip/node loc)
        (recur (zip/next loc))))))

(m/=> import-svg [:-> App SvgData App])
(defn import-svg
  [db data]
  (let [{:keys [svg label position]} data
        [x y] position
        hickory (hickory/as-hickory (hickory/parse svg))
        zipper (hickory.zip/hickory-zip hickory)
        svg (find-svg zipper)
        svg (-> svg
                (assoc :label label)
                (update :attrs dissoc :desc :version :xmlns)
                (assoc-in [:attrs :x] x)
                (assoc-in [:attrs :y] y))]
    (-> (add db svg)
        (collapse-all))))

(m/=> snapping-points [:-> App [:maybe [:sequential Element]] [:vector Vec2]])
(defn snapping-points
  [db els]
  (let [options (-> db :snap :options)]
    (into [] (mapcat #(utils.element/acc-snapping-points % options)) els)))
