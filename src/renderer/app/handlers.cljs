(ns renderer.app.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App Feature Platform]]
   [renderer.utils.platform :as utils.platform]))

(m/=> add-fx [:-> App vector? App])
(defn add-fx
  [db effect]
  (update db :fx conj effect))

(m/=> supported-feature? [:-> App Feature boolean?])
(defn supported-feature?
  [db k]
  (contains? (:features db) k))

(m/=> platform [:-> App Platform])
(defn platform
  [db]
  (:platform db))

(m/=> desktop? [:-> App boolean?])
(defn desktop?
  [db]
  (utils.platform/desktop? (platform db)))

(m/=> mobile? [:-> App boolean?])
(defn mobile?
  [db]
  (utils.platform/mobile? (platform db)))

(m/=> web? [:-> App boolean?])
(defn web?
  [db]
  (utils.platform/web? (platform db)))
