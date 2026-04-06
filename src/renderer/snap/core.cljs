(ns renderer.snap.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.snap.events :as snap.events]
   [renderer.snap.subs :as snap.subs]))

(rf/dispatch [::action.events/register-action
              {:id :snap/toggle
               :label [::snap "Snap"]
               :icon "magnet"
               :event [::snap.events/toggle]
               :active [::snap.subs/enabled?]}])

(rf/dispatch [::action.events/register-action
              {:id :snap/toggle-centers
               :label [::centers "centers"]
               :icon "magnet"
               :event [::snap.events/toggle-option :centers]
               :active [::snap.subs/option-enabled? :centers]}])

(rf/dispatch [::action.events/register-action
              {:id :snap/toggle-midpoints
               :label [::midpoints "midpoints"]
               :icon "magnet"
               :event [::snap.events/toggle-option :midpoints]
               :active [::snap.subs/option-enabled? :midpoints]}])

(rf/dispatch [::action.events/register-action
              {:id :snap/toggle-corners
               :label [::corners "corners"]
               :icon "magnet"
               :event [::snap.events/toggle-option :corners]
               :active [::snap.subs/option-enabled? :corners]}])

(rf/dispatch [::action.events/register-action
              {:id :snap/toggle-nodes
               :label [::nodes "nodes"]
               :icon "magnet"
               :event [::snap.events/toggle-option :nodes]
               :active [::snap.subs/option-enabled? :nodes]}])
