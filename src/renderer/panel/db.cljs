(ns renderer.panel.db)

(def Panel
  [:map {:closed true}
   [:visible boolean?]])

(def PanelId
  [:enum :tree :attributes :timeline :xml :history :repl-history])

(def default
  {:tree {:visible true}
   :attributes {:visible true}
   :timeline {:visible false}
   :xml {:visible false}
   :history {:visible false}
   :repl-history {:visible false}})
