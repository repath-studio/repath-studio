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
  "Returns an interceptor that calls history.handlers/finalize on the db after
   the handler runs.

   Accepts either static explanation args:
     (finalize [::lock-selection \"Lock selection\"])
     (finalize [::set \"Set %1\"] [(name k)])

   Or a function that receives the event vector and returns the
   explanation args:
     (finalize (fn [[_ k]] [[::set \"Set %1\"] [(name k)]]))"
  [& explanation]
  (rf/->interceptor
   :id ::finalize
   :after (fn [context]
            (if-let [db (rf/get-effect context :db)]
              (let [now (.now js/performance)
                    event (rf/get-coeffect context :event)
                    expl (if (fn? (first explanation))
                           ((first explanation) event)
                           explanation)]
                (rf/assoc-effect context :db (apply history.handlers/finalize
                                                    db now expl)))
              context))))
