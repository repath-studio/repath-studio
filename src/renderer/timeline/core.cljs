(ns renderer.timeline.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.timeline.effects]
   [renderer.timeline.events :as timeline.events]
   [renderer.timeline.subs :as timeline.subs]))

(rf/dispatch [::action.events/register-action
              {:id :timeline/toggle-replay
               :label [::replay "Replay"]
               :icon "refresh"
               :active [::timeline.subs/replay?]
               :event [::timeline.events/toggle-replay]}])

(rf/dispatch [::action.events/register-action
              {:id :timeline/toggle-grid-snap
               :label [::grid-snap "Grid snap"]
               :icon "magnet"
               :active [::timeline.subs/grid-snap?]
               :event [::timeline.events/toggle-grid-snap]}])

(rf/dispatch [::action.events/register-action
              {:id :timeline/toggle-guide-snap
               :label [::guide-snap "Guide snap"]
               :icon "magnet"
               :active [::timeline.subs/guide-snap?]
               :event [::timeline.events/toggle-guide-snap]}])

(rf/dispatch [::action.events/register-action-group
              {:id :timeline
               :icon "animation"
               :label [::timeline "Timeline"]
               :actions [:timeline/toggle-replay]}])
