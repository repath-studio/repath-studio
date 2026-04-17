(ns renderer.ruler.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.handlers :as app.handlers]
   [renderer.effects :as-alias effects]
   [renderer.tool.handlers :as tool.handlers]))

(rf/reg-event-db
 ::activate-guide-tool
 (fn [db [_ orientation e]]
   (let [{:keys [tool state]} db]
     (cond-> db
       (:active-document db)
       (-> (assoc :cached-tool tool
                  :cached-state state)
           (tool.handlers/activate :guide :orientation orientation)
           (assoc-in [:active-pointers (.-pointerId e)] e)
           (app.handlers/add-fx [::effects/focus-canvas nil]))))))
