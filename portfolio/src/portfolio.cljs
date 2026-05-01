(ns portfolio
  (:require
   [pages.components]
   [portfolio.ui :as ui]))

(ui/start!
 {:config
  {:css-paths ["/styles.css"]
   :background/options [{:id :light-mode
                         :title "Light mode"
                         :value {:background/background-color "#fff"
                                 :background/document-data {:theme "light"}}}
                        {:id :dark-mode
                         :title "Dark mode"
                         :value {:background/background-color "#111"
                                 :background/document-data {:theme "dark"}}}]
   :background/default-option-id :dark-mode
   :viewport/defaults {:viewport/padding [0]
                       :viewport/width "100%"
                       :viewport/height "500px"}
   :canvas/gallery-defaults {:viewport/padding [0]
                             :viewport/width "100%"
                             :viewport/height 41}}})

(defn ^:export init! [])
