(ns renderer.document.handlers
  (:require
   [config :as config]
   [malli.core :as m]
   [malli.transform :as m.transform]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [Vec2]]
   [renderer.document.db
    :as document.db
    :refer [Document DocumentId DocumentTitle PersistedDocument RecentDocument]]
   [renderer.element.db :refer [ElementId]]
   [renderer.element.handlers :as element.handlers]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.utils.vec :as utils.vec]))

(m/=> path [:function
            [:-> App vector?]
            [:-> App keyword? vector?]
            [:-> App keyword? [:* any?] vector?]])
(defn path
  ([db]
   [:documents (:active-document db)])
  ([db k]
   (conj (path db) k))
  ([db k & more]
   (apply conj (path db) k more)))

(m/=> entity [:-> App DocumentId Document])
(defn entity
  [db id]
  (get-in db [:documents id]))

(m/=> active [:-> App Document])
(defn active
  [db]
  (get-in db (path db)))

(m/=> set-active [:-> App DocumentId App])
(defn set-active
  [db id]
  (-> (assoc db :active-document id)
      (snap.handlers/rebuild-tree)))

(m/=> persisted-format [:-> App DocumentId PersistedDocument])
(defn persisted-format
  [db id]
  (-> PersistedDocument
      (m/decode (entity db id) m.transform/strip-extra-keys-transformer)
      (assoc :version (:version db))
      (update :elements (fn [elements]
                          (into {}
                                (map (fn [[k v]] [k (dissoc v :selected)]))
                                elements)))))

(m/=> close [:-> App DocumentId App])
(defn close
  [db id]
  (let [{:keys [active-document document-tabs]} db
        index (.indexOf document-tabs id)
        new-index (if (zero? index) (inc index) (dec index))
        active-id (if (= active-document id)
                    (get document-tabs new-index)
                    active-document)]
    (-> (if active-id
          (set-active db active-id)
          (dissoc db :active-document))
        (update :document-tabs #(filterv (complement #{id}) %))
        (update :documents dissoc id))))

(m/=> add-recent [:-> App map? App])
(defn add-recent
  [db document]
  (let [max-recent 10
        equals? (fn [x] (or (and (:path x)
                                 (= (:path x) (:path document)))
                            (= (:id x) (:id document))))]
    (cond-> db
      (or (:path document)
          (:file-handle document))
      (update :recent #(->> (m/decode RecentDocument
                                      document
                                      m.transform/strip-extra-keys-transformer)
                            (conj (filterv (complement equals?) %))
                            (take-last max-recent)
                            (vec))))))

(m/=> remove-recent [:-> App DocumentId App])
(defn remove-recent
  [db id]
  (update db :recent #(filterv (fn [x] (not= id (:id x))) %)))

(m/=> center [:-> App App])
(defn center
  [db]
  (cond-> db
    (and (:active-document db)
         (-> db :dom-rect)
         (not (get-in db (path db :centered))))
    (-> (frame.handlers/focus-bbox :original)
        (assoc-in (path db :centered) true)
        (snap.handlers/update-viewport-tree))))

(m/=> new-title [:-> App DocumentTitle])
(defn new-title
  [db]
  (let [documents (vals (:documents db))
        existing-titles (->> documents (map :title) set)]
    (loop [n 1]
      (let [title (str "Untitled-" n)]
        (if (contains? existing-titles title)
          (recur (inc n))
          title)))))

(m/=> create-tab [:-> App Document App])
(defn create-tab
  [db document]
  (let [{:keys [id title]} document
        {:keys [document-tabs active-document]} db
        active-index (.indexOf document-tabs active-document)
        document (cond-> document
                   (not title)
                   (assoc :title (new-title db)))]
    (-> db
        (assoc-in [:documents id] document)
        (set-active id)
        (update :document-tabs #(utils.vec/add % (inc active-index) id)))))

(m/=> create [:function
              [:-> App uuid? App]
              [:-> App uuid? [:maybe Vec2] App]])
(defn create
  ([db guid]
   (create db guid [595 842]))
  ([db guid size]
   (-> db
       (create-tab (assoc document.db/default :id guid))
       (element.handlers/create-default-canvas size)
       (center))))

(m/=> set-hovered-ids [:-> App [:set ElementId] App])
(defn set-hovered-ids
  [db ids]
  (assoc-in db (path db :hovered-ids) ids))

(m/=> collapse-el [:-> App ElementId App])
(defn collapse-el
  [db id]
  (update-in db (path db :collapsed-ids) conj id))

(m/=> expand-el [:-> App ElementId App])
(defn expand-el
  [db id]
  (update-in db (path db :collapsed-ids) disj id))

(m/=> attr [:-> App keyword? string?])
(defn attr
  [db k]
  (get-in (active db) [:attrs k]))

(m/=> assoc-attr [:-> App keyword? string? App])
(defn assoc-attr
  [db k v]
  (assoc-in db (path db :attrs k) v))

(m/=> update-saved-history-index [:function
                                  [:-> App App]
                                  [:-> App DocumentId App]])
(defn update-saved-history-index
  ([db]
   (update-saved-history-index db (:active-document db)))
  ([db id]
   (let [position (get-in db [:documents id :history :position])]
     (assoc-in db [:documents id :saved-history-index] position))))

(m/=> search-by-path [:-> App string? [:maybe DocumentId]])
(defn search-by-path
  [db document-path]
  (->> (:documents db)
       (vals)
       (some #(when (= document-path (:path %))
                (:id %)))))

(m/=> saved? [:-> App DocumentId boolean?])
(defn saved?
  [db id]
  (let [document (get-in db [:documents id])
        history-position (get-in document [:history :position])]
    (= (:saved-history-index document) history-position)))

(m/=> open? [:-> App DocumentId boolean?])
(defn open?
  [db id]
  (some #{id} (:document-tabs db)))

(m/=> recent? [:-> App DocumentId boolean?])
(defn recent?
  [db id]
  (some #(= id (:id %)) (:recent db)))

(m/=> saved-ids [:-> App sequential?])
(defn saved-ids
  [db]
  (->> (:document-tabs db)
       (filter (partial saved? db))))

(m/=> ->save-format [:-> PersistedDocument string?])
(defn ->save-format
  [document]
  (-> (apply dissoc document config/save-info-keys)
      (pr-str)))
