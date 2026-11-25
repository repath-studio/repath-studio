(ns renderer.utils.dom
  (:require
   [clojure.string :as string]
   [malli.core :as m]
   [renderer.db :refer [JS_Object]]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.hierarchy :as hierarchy]))

(def svg-ns "http://www.w3.org/2000/svg")

(def canvas-id "canvas")
(def frame-id "frame")
(def shell-input-id "shell-input")

(defn get-shell-element
  []
  (.getElementById js/document shell-input-id))

(defn get-frame-document
  []
  (some->> frame-id
           (.getElementById js/document)
           (.-contentWindow)
           (.-document)))

(defn get-canvas-element
  []
  (some-> (get-frame-document)
          (.getElementById canvas-id)))

(defn get-svg-elements
  []
  (some-> (get-frame-document)
          (.querySelectorAll "svg")))

(defn get-animation-elements
  []
  (when-let [tags (-> (hierarchy/get-descendants ::element.hierarchy/animation)
                      (seq))]
    (some-> (get-frame-document)
            (.querySelectorAll (string/join "," (map name tags))))))

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
