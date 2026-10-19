(ns worker.core
  (:require
   ["imagetracerjs" :as ImageTracer]))

(defn ^:export init! []
  (js/self.addEventListener
   "message"
   (fn [^js e]
     (let [data (.-data e)]
       (case (.-action data)
         "trace"
         (let [svg (.imagedataToSVG ImageTracer (.-image data))]
           (js/postMessage #js{:svg svg
                               :label (str "Traced " (or (.-label data)
                                                         "image"))
                               :position (.-position data)
                               :size (.-size data)
                               :viewBox (.-viewBox data)
                               :preserveAspectRatio (.-preserveAspectRatio data)
                               :id (.-id data)}))

         (js/postMessage #js {:id (.-id data)}))))))
