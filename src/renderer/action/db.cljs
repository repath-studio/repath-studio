(ns renderer.action.db
  (:require
   [malli.core :as m]
   [renderer.i18n.db :refer [Translation]]))

(def ActionId keyword?)
(def ActionGroupId keyword?)

(def Shortcut
  [:map {:closed true}
   [:keyCode number?]
   [:ctrlKey {:optional true} boolean?]
   [:shiftKey {:optional true} boolean?]
   [:altKey {:optional true} boolean?]])

(def ActionGroup
  [:map {:closed true}
   [:id ActionGroupId]
   [:label Translation]
   [:enabled {:optional true} vector?]
   [:actions [:vector ActionId]]])

(def Action
  [:map {:closed true}
   [:id ActionId]
   [:label Translation]
   [:icon string?]
   [:event vector?]
   [:shortcuts {:optional true} [:vector Shortcut]]
   [:enabled {:optional true} vector?]
   [:available {:optional true} vector?]
   [:active {:optional true} vector?]])

(def valid-action? (m/validator Action))
(def valid-action-group? (m/validator ActionGroup))

(def explain-action (m/explainer Action))
(def explain-action-group (m/explainer ActionGroup))

(def ActionRegistry
  [:map-of ActionId Action])

(def ActionGroupRegistry
  [:map-of ActionGroupId ActionGroup])
