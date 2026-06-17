(ns renderer.tool.events
  (:require
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.events]
   [renderer.element.handlers :as element.handlers]
   [renderer.tool.db :as tool.db]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.impl.base.transform.core :as-alias tool.impl.base.transform]))

(rf/reg-event-db
 ::activate
 (fn [db [_ tool & {:as opts}]]
   (cond-> db
     (and (tool.db/tool? tool)
          (:active-document db))
     (tool.handlers/activate tool opts))))

(rf/reg-event-db
 ::edit
 (fn [db [_]]
   (tool.handlers/edit db)))

(rf/reg-event-db
 ::set-state
 (fn [db [_ state]]
   (tool.handlers/set-state db state)))

(rf/reg-event-fx
 ::cancel
 (fn [{:keys [db]} _]
   (if (and (= (:tool db) ::tool.impl.base.transform/transform)
            (= (:state db) :idle)
            (seq (element.handlers/selected db)))
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
