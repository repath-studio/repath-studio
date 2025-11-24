(ns renderer.document.db
  (:require
   [malli.core :as m]
   [malli.transform :as m.transform]
   [renderer.db :refer [Vec2 JS_Object]]
   [renderer.element.db :refer [Element ElementId]]
   [renderer.history.db :refer [History HistoryIndex]]
   [renderer.tool.db :refer [HandleId]]))

(def ZoomFactor
  [:and number? [:>= 0.01] [:<= 100]])

(def DocumentId uuid?)

(def DocumentTitle [:string {:min 1}])

(def Document
  [:map {:closed true}
   [:id {:optional true} DocumentId]
   [:title {:optional true} DocumentTitle]
   [:path {:optional true} string?]
   [:saved-history-index {:optional true} HistoryIndex]
   [:version {:optional true} string?]
   [:hovered-ids {:default #{}} [:set [:or HandleId ElementId]]]
   [:collapsed-ids {:default #{}} [:set ElementId]]
   [:ignored-ids {:default #{}} [:set [:or HandleId ElementId]]]
   [:zoom {:default 1} ZoomFactor]
   [:rotate {:default 0} number?]
   [:history {:optional true} History]
   [:pan {:default [0 0]} Vec2]
   [:elements {:default {}} [:map-of ElementId Element]]
   [:centered {:optional true} boolean?]
   [:attrs {:default {:fill "white"
                      :stroke "black"}} [:map-of keyword? string?]]
   [:preview-label {:optional true} string?]
   [:file-handle {:optional true} JS_Object]])

(def PersistedDocument
  (->> Document
       (m/children)
       (filter (comp #(some #{%} [:id :title :path :version :elements]) first))
       (into [:map {:closed true}])))

(def RecentDocument
  (->> Document
       (m/children)
       (filter (comp #(some #{%} [:id :title :path]) first))
       (into [:map {:closed true}])))

(def valid? (m/validator Document))

(def explain (m/explainer Document))

(def default (m/decode Document
                       {}
                       m.transform/default-value-transformer))
