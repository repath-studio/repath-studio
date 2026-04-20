(ns renderer.history.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.effects]
   [renderer.effects :as-alias effects]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]))

(rf/reg-event-db
 ::undo
 (fn [db _]
   (history.handlers/undo db)))

(rf/reg-event-db
 ::redo
 (fn [db _]
   (history.handlers/redo db)))

(rf/reg-event-db
 ::undo-by
 (fn [db [_ n]]
   (history.handlers/undo db n)))

(rf/reg-event-db
 ::redo-by
 (fn [db [_ n]]
   (history.handlers/redo db n)))

(rf/reg-event-db
 ::reset-state
 (fn [db _]
   (-> db
       (history.handlers/reset-state)
       (history.handlers/clear-preview-label))))

(rf/reg-event-fx
 ::preview
 [(rf/inject-cofx ::effects/time-origin)]
 (fn [{:keys [db time-origin]} [_ pos]]
   {:db (history.handlers/preview db time-origin pos)}))

(rf/reg-event-db
 ::go-to
 (fn [db [_ id]]
   (-> db
       (history.handlers/go-to id)
       (history.handlers/clear-preview-label))))

(rf/reg-event-db
 ::clear
 (fn [db _]
   (history.handlers/drop-rest db)))

(rf/reg-event-db
 ::tree-view-updated
 (fn [db [_ zoom translate]]
   (cond-> db
     zoom
     (history.handlers/set-zoom zoom)

     translate
     (history.handlers/set-translate translate))))

(rf/reg-global-interceptor
 (rf/->interceptor
  :id ::auto-persist
  :after (fn [context]
           (let [db (rf/get-effect context :db)
                 fx (rf/get-effect context :fx)
                 prev-position (when-let [db (rf/get-coeffect context :db)]
                                 (when (:active-document db)
                                   (history.handlers/position db)))]
             (cond-> context
               (and db (not= (history.handlers/position db) prev-position))
               (rf/assoc-effect :fx (conj (or fx [])
                                          [::app.effects/persist])))))))

(defn finalize
  "Returns an interceptor that:
   1. Checks if (:state db) is :idle, short-circuiting if not.
   2. Injects a performance timestamp into coeffects as :now.
   3. Activates the :transform tool on the db in coeffects.
   4. After the handler runs, calls history.handlers/finalize on the
      resulting db.

   Accepts either static explanation args:
     (finalize [::lock-selection \"Lock selection\"])
     (finalize [::set \"Set %1\"] [(name k)])

   Or a function that receives the event vector and returns the
   explanation args:
     (finalize (fn [[_ k]] [[::set \"Set %1\"] [(name k)]]))"
  [& explanation]
  (rf/->interceptor
   :id ::finalize
   :before (fn [context]
             (let [db (rf/get-coeffect context :db)]
               (if (or (= (:state db) :idle)
                       (not (:active-document db)))
                 (-> context
                     (rf/assoc-coeffect :now (.now js/performance))
                     (rf/assoc-coeffect :db
                                        (tool.handlers/activate db :transform)))
                 (assoc context :queue []))))
   :after (fn [context]
            (if-let [db (rf/get-effect context :db)]
              (let [now (rf/get-coeffect context :now)
                    event (rf/get-coeffect context :event)
                    expl (if (fn? (first explanation))
                           ((first explanation) event)
                           explanation)]
                (rf/assoc-effect context :db
                                 (apply history.handlers/finalize
                                        db now expl)))
              context))))
