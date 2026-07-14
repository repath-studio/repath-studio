(ns renderer.shell.reepl.handlers)

(defn clear-items
  [db]
  (assoc db :items []))

#_(defn init
    [db data]
    (merge db data))

#_(defn add-item
    [db item]
    (update db :items conj item))

#_(defn add-items
    [db items]
    (update db :items concat items))

(defn add-input
  [db input]
  (let [inum (count (:history db))]
    (-> db
        (assoc :hist-pos 0)
        (update :history conj "")
        (update :items conj {:type :input
                             :text input
                             :num inum}))))

(defn add-result
  [db error? value]
  (update db :items conj {:type (if error? :error :output)
                          :value value}))

(defn add-log
  [db v]
  (update db :items conj {:type :log
                          :value v}))

(defn set-text
  [db text]
  (let [history (:history db)
        pos (:hist-pos db)
        idx (- (count history) pos 1)]
    (assoc db
           :hist-pos 0
           :history (if (zero? pos)
                      (assoc history idx text)
                      (if (= "" (last history))
                        (assoc history (dec (count history)) text)
                        (conj history text))))))

(defn go-up
  [db]
  (let [pos (:hist-pos db)
        len (count (:history db))
        new-pos (if (>= pos (dec len))
                  pos
                  (inc pos))]
    (assoc db :hist-pos new-pos)))

(defn go-down
  [db]
  (let [pos (:hist-pos db)
        new-pos (if (<= pos 0)
                  0
                  (dec pos))]
    (assoc db :hist-pos new-pos)))

;; TODO: is there a macro or something that could do this cleaner?
(defn make-handlers
  [state]
  {:add-input (partial swap! state add-input)
   :add-result (partial swap! state add-result)
   :go-up (partial swap! state go-up)
   :go-down (partial swap! state go-down)
   :clear-items (partial swap! state clear-items)
   :set-text (partial swap! state set-text)
   :add-log (partial swap! state add-log)})
