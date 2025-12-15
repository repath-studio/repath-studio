(ns renderer.theme.db)

(def NativeMode
  [:enum :dark :light])

(def ThemeMode
  (conj NativeMode :system))

(def Theme
  [:map {:closed true}
   [:mode {:default :dark} ThemeMode]
   [:native-mode {:optional true} NativeMode]])
