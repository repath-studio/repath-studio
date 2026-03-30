(ns renderer.history.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [JS_Object Vec2]]
   [renderer.document.db :refer [DocumentId]]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.db :refer [History HistoryIndex HistoryState]]
   [renderer.i18n.db :refer [Translation]]
   [renderer.utils.vec :as utils.vec]))

(m/=> path [:function
            [:-> App vector?]
            [:-> App keyword? vector?]
            [:-> App keyword? [:* any?] vector?]])
(defn path
  ([db]
   [:documents (:active-document db) :history])
  ([db k]
   (conj (path db) k))
  ([db k & more]
   (apply conj (path db) k more)))

(m/=> history [:-> App [:maybe History]])
(defn history
  [db]
  (get-in db (path db)))

(m/=> position [:-> App [:maybe HistoryIndex]])
(defn position
  [db]
  (:position (history db)))

(m/=> state [:function
             [:-> [:maybe History] [:maybe HistoryState]]
             [:-> [:maybe History] [:maybe HistoryIndex]
              [:maybe HistoryState]]])
(defn state
  ([active-history]
   (state active-history (:position active-history)))
  ([active-history current-position]
   (get-in active-history [:states current-position])))

(m/=> previous-position [:-> History [:maybe HistoryIndex]])
(defn previous-position
  [active-history]
  (-> active-history state :parent))

(m/=> next-position [:-> History [:maybe HistoryIndex]])
(defn next-position
  [active-history]
  (-> active-history state :children last))

(m/=> undos? [:-> History boolean?])
(defn undos?
  [active-history]
  (boolean (and active-history (:parent (state active-history)))))

(m/=> redos? [:-> History boolean?])
(defn redos?
  [active-history]
  (boolean (and active-history (next-position active-history))))

(m/=> reset-state [:-> App App])
(defn reset-state
  [db]
  (cond-> db
    (:active-document db)
    (assoc-in (element.handlers/path db) (-> db history state :elements))))

(m/=> drop-rest [:function
                 [:-> App App]
                 [:-> App DocumentId App]])
(defn drop-rest
  "Drops all but the current state from history.
   Useful to avoid persisting the rest of the history."
  ([db]
   (reduce drop-rest db (:document-tabs db)))
  ([db document-id]
   (let [document (get-in db [:documents document-id])
         pos (get-in document [:history :position])
         saved-history-index (:saved-history-index document)]
     (cond-> db
       :always
       (update-in [:documents document-id] dissoc :saved-history-index)

       (= saved-history-index pos)
       (assoc-in [:documents document-id :saved-history-index] 0)

       pos
       (-> (assoc-in [:documents document-id :history :position] 0)
           (assoc-in [:documents document-id :history :states]
                     {0 (-> (get-in document [:history :states pos])
                            (dissoc :parent)
                            (assoc :index 0
                                   :children []))}))))))

(m/=> preview [:-> App HistoryIndex App])
(defn preview
  [db pos]
  (let [preview-state (-> db history (state pos))
        timestamp (-> preview-state :timestamp js/Date.)]
    (-> db
        (assoc-in (document.handlers/path db :preview-label) (str timestamp))
        (assoc-in (element.handlers/path db) (:elements preview-state)))))

(m/=> go-to [:-> App HistoryIndex App])
(defn go-to
  [db pos]
  (-> db
      (assoc-in (path db :position) pos)
      (reset-state)))

(m/=> undo [:function
            [:-> App App]
            [:-> App pos-int? App]])
(defn undo
  ([db]
   (undo db 1))
  ([db n]
   (if-not (and (pos? n)
                (undos? (history db)))
     db
     (recur (go-to db (previous-position (history db)))
            (dec n)))))

(m/=> redo [:function
            [:-> App App]
            [:-> App pos-int? App]])
(defn redo
  ([db]
   (redo db 1))
  ([db n]
   (if-not (and (pos? n)
                (redos? (history db)))
     db
     (recur (go-to db (next-position (history db)))
            (dec n)))))

(m/=> accumulate [:-> History ifn? [:vector HistoryState]])
(defn accumulate
  [active-history f]
  (loop [current-state (state active-history)
         stack []]
    (if-let [current-position (f current-state)]
      (let [accumulated-state (state active-history current-position)
            accumulated-state (dissoc accumulated-state :elements)]
        (recur accumulated-state (conj stack accumulated-state)))
      stack)))

(m/=> undos [:-> History [:vector HistoryState]])
(defn undos
  [active-history]
  (accumulate active-history :parent))

(m/=> redos [:-> History [:vector HistoryState]])
(defn redos
  [active-history]
  (accumulate active-history (fn [current-state]
                               (-> current-state :children last))))

(m/=> state-count [:-> App int?])
(defn state-count
  [db]
  (-> db history :states count))

(m/=> set-zoom [:-> App number? App])
(defn set-zoom
  [db, zoom]
  (assoc-in db (path db :zoom) zoom))

(m/=> set-translate [:-> App Vec2 App])
(defn set-translate
  [db [x y]]
  (cond-> db
    (and x y)
    (assoc-in (path db :translate) [x y])))

(m/=> clear-preview-label [:-> App App])
(defn clear-preview-label
  [db]
  (update-in db [:documents (:active-document db)] dissoc :preview-label))

(m/=> create-state [:-> App HistoryIndex Translation HistoryState])
(defn create-state
  [db index explanation]
  (let [new-state {:explanation explanation
                   :elements (get-in db (element.handlers/path db))
                   :timestamp (.now js/Date) ; REVIEW: Sideffect
                   :index index
                   :children []}]
    (cond-> new-state
      (position db)
      (assoc :parent (position db)))))

(m/=> age-ratio->color [:-> number? string?])
(defn age-ratio->color
  "Computes a color from age-ratio (/ age max-age).
   https://developer.mozilla.org/en-US/docs/Web/CSS/hue"
  [ratio]
  (let [start-hue 20 ; Brownish/Orange
        end-hue 120 ; Green
        hue (+ start-hue (* ratio (- end-hue start-hue)))]
    (str "hsl(" hue ", 40%, 60%)")))

(m/=> state->d3-data [:-> History [:maybe HistoryIndex] JS_Object])
(defn state->d3-data
  [active-history saved-index]
  (let [states (:states active-history)
        n (count states)]
    (loop [queue [[0 nil]]
           result-map {}]
      (if (empty? queue)
        (get result-map 0)
        (let [[index parent-obj] (first queue)
              {:keys [index explanation children]} (get states index)
              js-node #js {:name explanation
                           :id index
                           :saved (= index saved-index)
                           :active (= index (:position active-history))
                           :color (age-ratio->color (/ index n))
                           :children #js []}]
          (when parent-obj
            (.push (.-children parent-obj) js-node))
          (recur (into (rest queue)
                       (map (fn [child-id] [child-id js-node]) children))
                 (assoc result-map index js-node)))))))

(m/=> update-ancestors [:-> App App])
(defn update-ancestors
  "Makes all ancestors of the active branch the rightmost element.
   This ensures that users will stay in the latest branch when they undo/redo."
  [db]
  (loop [db db
         node (state (history db))]
    (let [parent-index (:parent node)
          parent (state (history db) parent-index)]
      (if-not parent
        db
        (let [index (.indexOf (:children parent) (:index node))
              new-index (dec (count (:children parent)))
              children-path (path db :states parent-index :children)]
          (recur (update-in db children-path utils.vec/move index new-index)
                 parent))))))

(m/=> finalize [:-> App Translation App])
(defn finalize
  "Pushes changes to history."
  [db & explanation]
  (if (= (get-in db (element.handlers/path db))
         (-> db history state :elements))
    db
    (let [current-position (position db)
          index (state-count db)]
      (-> db
          (assoc-in (path db :position) index)
          (assoc-in (path db :states index)
                    (create-state db index explanation))
          (cond->
           current-position
            (-> (update-in (path db :states current-position :children)
                           conj index)
                (update-ancestors)))))))
