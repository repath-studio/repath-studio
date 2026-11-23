(ns renderer.tree.events
  (:require
   [re-frame.core :as rf]
   [renderer.tree.effects :as-alias tree.effects]))

(rf/reg-event-fx
 ::focus
 (fn [_ [_ tree-ref]]
   {::tree.effects/focus tree-ref}))

(rf/reg-event-fx
 ::focus-first
 (fn [_ [_ tree-ref]]
   {::tree.effects/focus-first tree-ref}))

(rf/reg-event-fx
 ::focus-last
 (fn [_ [_ tree-ref]]
   {::tree.effects/focus-last tree-ref}))

(rf/reg-event-fx
 ::focus-up
 (fn [_ [_ id tree-ref]]
   {::tree.effects/focus-next [id :up tree-ref]}))

(rf/reg-event-fx
 ::focus-down
 (fn [_ [_ id tree-ref]]
   {::tree.effects/focus-next [id :down tree-ref]}))

(rf/reg-event-fx
 ::select-range
 (fn [_ [_ last-focused-id id tree-ref]]
   {::tree.effects/select-range [last-focused-id id tree-ref]}))
