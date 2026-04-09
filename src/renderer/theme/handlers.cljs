(ns renderer.theme.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.theme.db :refer [ThemeMode NativeMode]]))

(m/=> set-mode [:-> App ThemeMode App])
(defn set-mode
  [db mode]
  (assoc-in db [:theme :mode] mode))

(m/=> set-native-mode [:-> App NativeMode App])
(defn set-native-mode
  [db native-mode]
  (assoc-in db [:theme :native-mode] native-mode))

(m/=> compute-mode [:-> ThemeMode [:maybe NativeMode] [:maybe NativeMode]])
(defn compute-mode
  [mode native-mode]
  (if (= mode :system)
    native-mode
    mode))

(m/=> computed-mode [:-> App [:maybe NativeMode]])
(defn computed-mode
  [db]
  (let [mode (-> db :theme :mode)
        native-mode (-> db :theme :native-mode)]
    (compute-mode mode native-mode)))
