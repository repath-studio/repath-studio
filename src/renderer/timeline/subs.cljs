(ns renderer.timeline.subs
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.hierarchy :as hierarchy]
   [renderer.utils.clock :as utils.clock]))

(rf/reg-sub
 ::animations
 :<- [::element.subs/entities]
 (fn [elements]
   (->> elements
        (filter #(contains? (descendants @hierarchy/hierarchy
                                         ::element.hierarchy/animation)
                            (:tag %))))))

(defn effect-id
  [el]
  (str "effect" (:id el)))

(defn attr->seconds
  [v]
  (some-> v utils.clock/->ms (/ 1000)))

(defn animation->timeline-row
  [{:keys [id tag attrs selected locked visible]
    :as el}]
  (let [{:keys [begin dur end attributeName]} attrs
        start (or (attr->seconds begin) 0)
        dur (attr->seconds dur)
        end (or (attr->seconds end) (+ start dur))]
    {:id id
     :selected selected
     :actions [{:id (str id)
                :selected selected
                :flexible (not locked)
                :movable (not locked)
                :hidden (not visible)
                :name (string/join " " [(name tag) attributeName])
                :start start
                :end end
                :effectId (effect-id el)}]}))

(rf/reg-sub
 ::rows
 :<- [::animations]
 (fn [animations]
   (->> animations
        (mapv animation->timeline-row)
        (clj->js))))

(rf/reg-sub
 ::end
 :<- [::rows]
 (fn [rows]
   (reduce #(max (-> %2 .-actions first .-end) %1) 0 rows)))

(defn animation->effect
  [el]
  {:id (effect-id el)})

(rf/reg-sub
 ::effects
 :<- [::animations]
 (fn [animations]
   (->> animations
        (reduce #(assoc %1 (effect-id %2) (animation->effect %2)) {})
        (clj->js))))

(defn pad-2
  [n]
  (-> n str js/parseInt str (.padStart 2 "0")))

(rf/reg-sub
 ::timeline
 :-> :timeline)

(rf/reg-sub
 ::time
 :<- [::timeline]
 :-> :time)

(rf/reg-sub
 ::time-formatted
 :<- [::time]
 (fn [t]
   (let [m (-> t (/ 60) pad-2)
         s (-> t (rem 60) pad-2)
         ms (-> t (rem 1) (* 100) pad-2 (string/replace "0." ""))]
     (str m ":" s ":" ms))))

(rf/reg-sub
 ::paused?
 :<- [::timeline]
 :-> :paused)

(rf/reg-sub
 ::grid-snap?
 :<- [::timeline]
 :-> :grid-snap)

(rf/reg-sub
 ::guide-snap?
 :<- [::timeline]
 :-> :guide-snap)

(rf/reg-sub
 ::replay?
 :<- [::timeline]
 :-> :replay)

(rf/reg-sub
 ::fit-duration?
 :<- [::timeline]
 :-> :fit-duration)

(rf/reg-sub
 ::speed
 :<- [::timeline]
 :-> :speed)
