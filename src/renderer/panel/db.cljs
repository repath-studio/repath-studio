(ns renderer.panel.db)

(def Panel
  [:map {:closed true}
   [:visible boolean?]])

(def PanelId
  [:enum :tree :attributes :timeline :xml :history :shell-output])

(def default
  {:tree {:visible true}
   :attributes {:visible true}
   :timeline {:visible false}
   :xml {:visible false}
   :history {:visible false}
   :shell-output {:visible false}})
