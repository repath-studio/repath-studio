(ns renderer.element.db
  (:require
   [malli.core :as m]
   [malli.transform :as m.transform]
   [renderer.db :refer [BBox]]
   [renderer.element.hierarchy :as element.hierarchy]))

(defn tag?
  [k]
  (contains? (descendants @element.hierarchy/hierarchy
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
   [:id {:optional true} ElementId]
   [:tag ElementTag]
   [:label {:optional true} string?]
   [:parent {:optional true} ElementId]
   [:type {:optional true} [:= :element]]
   [:visible {:optional true} boolean?]
   [:locked {:optional true} boolean?]
   [:selected {:optional true} boolean?]
   [:children {:optional true} [:vector ElementId]]
   [:bbox {:optional true} BBox]
   [:content {:optional true} string?]
   [:attrs {:optional true} ElementAttrs]])

(def valid? (m/validator Element))

(def explain (m/explainer Element))

(def default (m/decode Element
                       {:type :element
                        :visible true
                        :children []}
                       m.transform/default-value-transformer))
