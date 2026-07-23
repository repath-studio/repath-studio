(ns renderer.shell.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [LoadingState]]
   [renderer.shell.db
    :as shell.db
    :refer [ShellHistory ShellItem ShellLanguageId]]
   [renderer.utils.math :as utils.math]))

(def max-items 50)

(m/=> active-language [:-> App ShellLanguageId])
(defn active-language
  [db]
  (get-in db [:shell :active-language]))

(m/=> set-language [:-> App ShellLanguageId App])
(defn set-language
  [db lang]
  (assoc-in db [:shell :active-language] lang))

(m/=> init-language [:-> App App])
(defn init-language
  [db]
  (update-in db [:shell :languages (active-language db)]
             #(merge shell.db/default-lang %)))

(m/=> language-status [:-> App ShellLanguageId [:maybe LoadingState]])
(defn language-status
  [db lang]
  (get-in db [:shell :languages lang :status]))

(m/=> set-language-status [:-> App LoadingState App])
(defn set-language-status
  [db status]
  (assoc-in db [:shell :languages (active-language db) :status] status))

(m/=> reset-language-statuses [:-> App App])
(defn reset-language-statuses
  [db]
  (->> (keys (-> db :shell :languages))
       (reduce #(update-in %1 [:shell :languages %2] dissoc :status) db)))

(m/=> verbose? [:-> App boolean?])
(defn verbose?
  [db]
  (get-in db [:shell :verbose]))

(m/=> history [:-> App [:maybe ShellHistory]])
(defn history
  [db]
  (get-in db [:shell :languages (active-language db) :history]))

(m/=> reset-history-position [:-> App App])
(defn reset-history-position
  [db]
  (assoc-in db [:shell :languages (active-language db) :history-pos] 0))

(m/=> add-to-history [:-> App string? App])
(defn add-to-history
  [db text]
  (update-in db [:shell :languages (active-language db) :history]
             #(->> (conj % text)
                   (take-last max-items)
                   (vec))))

(m/=> clear-items [:-> App App])
(defn clear-items
  [db]
  (assoc-in db [:shell :languages (active-language db) :items] []))

(m/=> serializable-item? [:-> ShellItem boolean?])
(defn serializable-item?
  [item]
  (or (not= (:type item) :output)
      (string? (:value item))))

(m/=> serialize-items [:-> App App])
(defn serialize-items
  "Removes all :output items from the shell output history."
  [db]
  (update-in db [:shell :languages]
             (fn [langs]
               (reduce-kv (fn [acc lang data]
                            (assoc acc lang
                                   (update data :items
                                           (fn [items]
                                             (filterv serializable-item?
                                                      items)))))
                          {}
                          langs))))

(m/=> set-verbose [:-> App boolean? App])
(defn toggle-verbose
  [db]
  (update-in db [:shell :verbose] not))

(m/=> add-item [:-> App ShellItem App])
(defn add-item
  [db item]
  (update-in db [:shell :languages (active-language db) :items]
             #(->> (conj % item)
                   (take-last max-items)
                   (vec))))

(m/=> set-text [:-> App string? App])
(defn set-text
  [db text]
  (let [lang (active-language db)
        hist (history db)
        pos (get-in db [:shell :languages lang :history-pos])
        idx (- (count hist) pos 1)]
    (-> db
        (reset-history-position)
        (assoc-in [:shell :languages lang :history]
                  (if (zero? pos)
                    (assoc hist idx text)
                    (if (= "" (last hist))
                      (assoc hist (dec (count hist)) text)
                      (conj hist text)))))))

(m/=> update-history-position [:-> App ifn? App])
(defn update-history-position
  [db f]
  (let [max-pos (-> db history count dec)]
    (update-in db [:shell :languages (active-language db) :history-pos]
               (comp #(utils.math/clamp % 0 max-pos) f))))
