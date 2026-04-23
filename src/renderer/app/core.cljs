(ns renderer.app.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.app.effects]
   [renderer.app.events :as app.events]
   [renderer.app.subs :as app.subs]
   [renderer.tool.subs :as tool.subs]
   [renderer.utils.key :as utils.key]))

(rf/dispatch [::action.events/register-action
              {:id :view/toggle-grid
               :label [::grid "Grid"]
               :icon "grid"
               :event [::app.events/toggle-grid]
               :active [::app.subs/grid?]
               :shortcuts [{:keyCode (utils.key/codes "PERIOD")
                            :ctrlKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :view/toggle-rulers
               :label [::rulers "Rulers"]
               :icon "ruler-combined"
               :event [::app.events/toggle-rulers]
               :active [::app.subs/rulers?]
               :shortcuts [{:keyCode (utils.key/codes "R")
                            :ctrlKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :view/toggle-guides
               :label [::guides "Guides"]
               :icon "ruler-straight"
               :event [::app.events/toggle-guides]
               :active [::app.subs/guides?]
               :shortcuts [{:keyCode (utils.key/codes "PERIOD")
                            :shiftKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :view/toggle-guides-locked
               :label [::lock-guides "Lock guides"]
               :icon "ruler-straight"
               :event [::app.events/toggle-guides-locked]
               :enabled [::tool.subs/not-active? :guide]
               :active [::app.subs/guides-locked?]}])

(rf/dispatch [::action.events/register-action
              {:id :view/toggle-help-bar
               :label [::help-bar "Help bar"]
               :icon "info"
               :active [::app.subs/help-bar]
               :event [::app.events/toggle-help-bar]}])

(rf/dispatch [::action.events/register-action
              {:id :view/toggle-debug-info
               :label [::debug-info "Debug info"]
               :icon "bug"
               :event [::app.events/toggle-debug-info]
               :active [::app.subs/debug-info]
               :shortcuts [{:keyCode (utils.key/codes "D")
                            :ctrlKey true
                            :shiftKey true}]}])
