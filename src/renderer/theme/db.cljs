(ns renderer.theme.db)

(def NativeMode
  [:enum :dark :light])

(def ThemeMode
  [:enum :dark :light :system])

(def Theme
  [:map {:closed true}
   [:mode {:default :dark} ThemeMode]
   [:native-mode {:optional true} NativeMode]])
