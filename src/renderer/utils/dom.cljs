(ns renderer.utils.dom
  (:require
   [malli.core :as m]
   [renderer.db :refer [JS_Object]]))

(defn get-frame-document
  []
  (some-> (.getElementById js/document "frame")
          (.-contentWindow)
          (.-document)))

(defn get-canvas-element
  []
  (some-> (get-frame-document)
          (.getElementById "canvas")))

(m/=> event->uuid [:-> JS_Object [:maybe uuid?]])
(defn event->uuid
  [e]
  (some-> (.-dataTransfer e)
          (.getData "id")
          (uuid)))

(m/=> content-overflow? [:-> JS_Object boolean?])
(defn content-overflow?
  [el]
  (boolean (and el (or (> (.-scrollWidth el) (.-clientWidth el))
                       (> (.-scrollHeight el) (.-clientHeight el))))))
