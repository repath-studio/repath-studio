(ns renderer.app.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App Feature]]
   [renderer.utils.platform :as utils.platform]))

(m/=> add-fx [:-> App vector? App])
(defn add-fx
  [db effect]
  (update db :fx conj effect))

(m/=> supported-feature? [:-> App Feature boolean?])
(defn supported-feature?
  [db k]
  (contains? (:features db) k))

(m/=> desktop? [:-> App boolean?])
(defn desktop?
  [db]
  (-> db :platform utils.platform/desktop?))

(m/=> mobile? [:-> App boolean?])
(defn mobile?
  [db]
  (-> db :platform utils.platform/mobile?))

(m/=> web? [:-> App boolean?])
(defn web?
  [db]
  (-> db :platform utils.platform/web?))
