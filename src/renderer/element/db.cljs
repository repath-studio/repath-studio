(ns renderer.element.db
  (:require
   [malli.core :as m]
   [malli.transform :as m.transform]
   [renderer.db :refer [BBox]]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.hierarchy :as hierarchy]))

(defn tag?
  [k]
  (contains? (descendants @hierarchy/hierarchy
                          ::element.hierarchy/element) k))

(def ElementTag
  [:fn {:error/fn (fn [{:keys [value]} _]
                    (str value ", is not a supported tag"))}
   tag?])

(def image-mime-types
  {"image/png" [".png"]
   "image/jpeg" [".jpeg" ".jpg"]
   "image/bmp" [".bmp"]
   "image/gif" [".gif"]
   "image/webp" [".webp"]})

(def AnimationTag
  [:enum :animate :animateTransform :animateMotion])

(def ElementAttrs
  [:map-of keyword? string?])

(def Direction
  [:enum :top :center-vertical :bottom :left :center-horizontal :right])

(def ElementId uuid?)

(def Element
  [:map {:closed true}
   [:id {:optional true
         :persist true} ElementId]
   [:tag {:persist true} ElementTag]
   [:label {:optional true
            :persist true} string?]
   [:parent {:optional true
             :persist true} ElementId]
   [:type {:optional true
           :persist true} [:= :element]]
   [:visible {:optional true
              :persist true} boolean?]
   [:locked {:optional true
             :persist true} boolean?]
   [:selected {:optional true} boolean?]
   [:selected-handles {:optional true} [:set keyword?]]
   [:children {:optional true
               :persist true} [:vector ElementId]]
   [:bbox {:optional true
           :persist true} BBox]
   [:content {:optional true
              :persist true} string?]
   [:attrs {:optional true
            :persist true} ElementAttrs]])

(def valid? (m/validator Element))

(def explain (m/explainer Element))

(def PersistedElement
  (->> Element
       (m/children)
       (filter (comp :persist second))
       (into [:map {:closed true}])))

(def default (m/decode Element
                       {:type :element
                        :visible true
                        :selected-handles #{}
                        :children []}
                       m.transform/default-value-transformer))
