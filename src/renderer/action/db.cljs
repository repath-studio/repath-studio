(ns renderer.action.db
  (:require
   [malli.core :as m]
   [renderer.i18n.db :refer [Translation]]))

(def ActionId keyword?)

(def Shortcut
  [:map {:closed true}
   [:keyCode number?]
   [:ctrlKey {:optional true} boolean?]
   [:shiftKey {:optional true} boolean?]
   [:altKey {:optional true} boolean?]])

(def Action
  [:map {:closed true}
   [:id ActionId]
   [:label Translation]
   [:icon string?]
   [:event vector?]
   [:shortcuts {:optional true} [:vector Shortcut]]
   [:enabled {:optional true} vector?]
   [:available {:optional true} vector?]
   [:checked {:optional true} vector?]])

(def valid-action? (m/validator Action))

(def explain-action (m/explainer Action))

(def ActionRegistry
  [:map-of ActionId Action])
