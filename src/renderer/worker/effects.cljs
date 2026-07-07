(ns renderer.worker.effects
  (:require
   [re-frame.core :as rf]
   [renderer.worker.events :as-alias worker.events]))

(rf/reg-fx
 ::post
 (fn [{:keys [data on-success on-error]}]
   (let [worker (js/Worker. "js/worker.js")
         id (uuid (:id data))]
     (.addEventListener worker "message"
                        #(let [data (-> (.. % -data)
                                        (js->clj :keywordize-keys true))]
                           (rf/dispatch [::worker.events/message
                                         id
                                         on-success
                                         data])))

     (.addEventListener worker "error"
                        #(rf/dispatch [::worker.events/message id on-error %]))

     (.postMessage worker (clj->js data)))))
