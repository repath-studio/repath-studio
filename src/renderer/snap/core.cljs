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
               :label [::centers "Centers"]
               :icon "magnet"
               :event [::snap.events/toggle-option :centers]
               :active [::snap.subs/option-enabled? :centers]}])

(rf/dispatch [::action.events/register-action
              {:id :snap/toggle-midpoints
               :label [::midpoints "Midpoints"]
               :icon "magnet"
               :event [::snap.events/toggle-option :midpoints]
               :active [::snap.subs/option-enabled? :midpoints]}])

(rf/dispatch [::action.events/register-action
              {:id :snap/toggle-corners
               :label [::corners "Corners"]
               :icon "magnet"
               :event [::snap.events/toggle-option :corners]
               :active [::snap.subs/option-enabled? :corners]}])

(rf/dispatch [::action.events/register-action
              {:id :snap/toggle-nodes
               :label [::nodes "Nodes"]
               :icon "magnet"
               :event [::snap.events/toggle-option :nodes]
               :active [::snap.subs/option-enabled? :nodes]}])

(rf/dispatch [::action.events/register-action
              {:id :snap/toggle-grid
               :label [::grid "Grid"]
               :icon "magnet"
               :event [::snap.events/toggle-option :grid]
               :active [::snap.subs/option-enabled? :grid]}])

(rf/dispatch [::action.events/register-action
              {:id :snap/toggle-guides
               :label [::guides "Guides"]
               :icon "magnet"
               :event [::snap.events/toggle-option :guides]
               :active [::snap.subs/option-enabled? :guides]}])

(rf/dispatch [::action.events/register-action-group
              {:id :snap/options
               :label [::options "options"]
               :actions [:snap/toggle-centers
                         :snap/toggle-midpoints
                         :snap/toggle-corners
                         :snap/toggle-nodes
                         ;; :snap/toggle-guides
                         ;; :snap/toggle-grid
                         ]}])
