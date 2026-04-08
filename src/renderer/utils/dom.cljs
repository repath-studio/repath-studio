(ns renderer.utils.dom
  (:require
   [malli.core :as m]
   [renderer.db :refer [JS_Object]]))

(defn frame-document!
  []
  (some-> (.getElementById js/document "frame")
          (.-contentWindow)
          (.-document)))

(defn canvas-element!
  []
  (some-> (frame-document!)
          (.getElementById "canvas")))

(m/=> event->uuid [:-> JS_Object [:maybe uuid?]])
(defn event->uuid
  [e]
  (some-> (.-dataTransfer e)
          (.getData "id")
          (uuid)))

(defn content-overflow?
  [el]
  (and el (or (> (.-scrollWidth el) (.-clientWidth el))
              (> (.-scrollHeight el) (.-clientHeight el)))))
