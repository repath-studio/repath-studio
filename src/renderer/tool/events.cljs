(ns renderer.tool.events
  (:require
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.events]
   [renderer.tool.handlers :as tool.handlers]))

(rf/reg-event-db
 ::activate
 (fn [db [_ tool]]
   (cond-> db
     (:active-document db)
     (tool.handlers/activate tool))))

(rf/reg-event-db
 ::set-state
 (fn [db [_ state]]
   (tool.handlers/set-state db state)))

(rf/reg-event-fx
 ::cancel
 (fn [{:keys [db]} _]
   (if (and (= (:tool db) :transform)
            (= (:state db) :idle))
     {:dispatch [::element.events/deselect-all]}
     {:db (tool.handlers/cancel db)})))

(rf/reg-global-interceptor
 (rf/->interceptor
  :id ::custom-fx
  :after (fn [context]
           (let [db (rf/get-effect context :db)
                 fx (rf/get-effect context :fx)]
             (cond-> context
               db
               (-> (rf/assoc-effect :fx (apply conj (or fx []) (:fx db)))
                   (rf/assoc-effect :db (assoc db :fx []))))))))
