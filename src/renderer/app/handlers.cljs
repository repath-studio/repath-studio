(ns renderer.app.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App Feature]]
   [renderer.app.migrations :as app.migrations]
   [renderer.utils.compatibility :as utils.compatibility]
   [renderer.utils.platform :as utils.platform]))

(m/=> enqueue-fx [:-> App vector? App])
(defn enqueue-fx
  "Enqueues an effect when we are in the middle of a pure db transformation.
   The addition is handled by `::renderer.app.events/enqueue-fx` interceptor."
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

(m/=> migrate [:-> map? App])
(defn migrate
  [db]
  (utils.compatibility/migrate db app.migrations/migrations))
