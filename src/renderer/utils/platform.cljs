(ns renderer.utils.platform
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [Platform]]))

(m/=> desktop? [:-> Platform boolean?])
(defn desktop?
  [platform]
  (contains? #{"darwin" "win32" "linux"} platform))

(m/=> mobile? [:-> Platform boolean?])
(defn mobile?
  [platform]
  (contains? #{"android" "ios"} platform))

(m/=> web? [:-> Platform boolean?])
(defn web?
  [platform]
  (= platform "web"))

(m/=> mac? [:-> Platform boolean?])
(defn mac?
  [platform]
  (= platform "darwin"))
