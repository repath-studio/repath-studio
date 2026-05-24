(ns renderer.utils.dom
  (:require
   [config :as config]
   [malli.core :as m]
   [renderer.db :refer [JS_Object]]))

(defn get-frame-document
  []
  (some-> (.getElementById js/document config/frame-id)
          (.-contentWindow)
          (.-document)))

(defn get-canvas-element
  []
  (some-> (get-frame-document)
          (.getElementById config/canvas-id)))

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
