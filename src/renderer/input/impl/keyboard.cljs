(ns renderer.input.impl.keyboard
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.db :refer [JS_Object]]
   [renderer.input.db :refer [KeyboardEvent]]
   [renderer.input.events :as-alias input.events]))

(m/=> ->clj [:-> JS_Object KeyboardEvent])
(defn ->clj
  "https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent"
  [^js/KeyboardEvent e]
  {:target (.-target e)
   :type (.-type e)
   :code (.-code e)
   :key-code (.-keyCode e)
   :key (.-key e)
   :alt-key (.-altKey e)
   :ctrl-key (.-ctrlKey e)
   :meta-key (.-metaKey e)
   :shift-key (.-shiftKey e)})

(m/=> handler! [:-> JS_Object nil?])
(defn handler!
  [^js/KeyboardEvent e]
  (rf/dispatch-sync [::input.events/keyboard (->clj e)]))
