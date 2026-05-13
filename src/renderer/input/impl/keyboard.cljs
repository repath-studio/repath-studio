(ns renderer.input.impl.keyboard
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.db :refer [JS_Object]]
   [renderer.input.db :refer [KeyboardEvent]]
   [renderer.input.events :as-alias input.events]
   [renderer.input.hierarchy :as input.hierarchy]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.pan :as-alias tool.impl.base.pan]))

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

(defmethod input.hierarchy/keyboard "keydown"
  [db e]
  (cond-> db
    (and (= (:code e) "Space")
         (not= (:tool db) ::tool.impl.base.pan/pan))
    (tool.handlers/set-cached ::tool.impl.base.pan/pan)

    (and (= (:code e) "KeyX")
         (not (-> db :snap :transient-active)))
    (-> (assoc-in [:snap :transient-active] true)
        (cond->
         (not (-> db :snap :active))
          (-> (dissoc :nearest-neighbor)
              (snap.handlers/rebuild-tree))))

    (and (= (:key e) "Alt")
         (= (:state db) :idle))
    (assoc-in [:menubar :indicator] true)

    :always
    (tool.hierarchy/on-key-down e)))

(defmethod input.hierarchy/keyboard "keyup"
  [db e]
  (cond-> db
    (and (= (:code e) "Space")
         (:cached-tool db))
    (tool.handlers/reset-cached)

    (= (:code e) "KeyX")
    (-> (assoc-in [:snap :transient-active] false)
        (cond->
         (not (-> db :snap :active))
          (dissoc :nearest-neighbor)))

    (= (:key e) "Alt")
    (assoc-in [:menubar :indicator] false)

    :always
    (tool.hierarchy/on-key-up e)))
