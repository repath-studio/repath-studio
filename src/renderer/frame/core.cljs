(ns renderer.frame.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.document.subs :as document.subs]
   [renderer.frame.events :as frame.events]
   [renderer.frame.subs]
   [renderer.utils.key :as utils.key]))

(rf/dispatch [::action.events/register-action
              {:id :zoom/in
               :label [::zoom-in "In"]
               :icon "zoom-in"
               :event [::frame.events/zoom-in]
               :enabled [::document.subs/zoom-in-available?]
               :shortcuts [{:keyCode (utils.key/codes "EQUALS")}]}])

(rf/dispatch [::action.events/register-action
              {:id :zoom/out
               :label [::zoom-out "Out"]
               :icon "zoom-out"
               :event [::frame.events/zoom-out]
               :enabled [::document.subs/zoom-out-available?]
               :shortcuts [{:keyCode (utils.key/codes "DASH")}]}])

(rf/dispatch [::action.events/register-action
              {:id :zoom/set-50
               :label [::zoom-set-50 "Set to 50%"]
               :icon "magnifier"
               :enabled [::document.subs/entities?]
               :event [::frame.events/set-zoom 0.5]}])

(rf/dispatch [::action.events/register-action
              {:id :zoom/set-100
               :label [::zoom-set-100 "Set to 100%"]
               :icon "magnifier"
               :enabled [::document.subs/entities?]
               :event [::frame.events/set-zoom 1]}])

(rf/dispatch [::action.events/register-action
              {:id :zoom/set-200
               :label [::zoom-set-200 "Set to 200%"]
               :icon "magnifier"
               :enabled [::document.subs/entities?]
               :event [::frame.events/set-zoom 2]}])

(rf/dispatch [::action.events/register-action
              {:id :zoom/focus-selected
               :label [::zoom-focus-selected "Focus selected"]
               :icon "focus"
               :enabled [::document.subs/entities?]
               :event [::frame.events/focus-selection :original]
               :shortcuts [{:keyCode (utils.key/codes "ONE")}]}])

(rf/dispatch [::action.events/register-action
              {:id :zoom/fit-selected
               :label [::zoom-fit-selected "Fit selected"]
               :icon "focus"
               :enabled [::document.subs/entities?]
               :event [::frame.events/focus-selection :fit]
               :shortcuts [{:keyCode (utils.key/codes "TWO")}]}])

(rf/dispatch [::action.events/register-action
              {:id :zoom/fill-selected
               :label [::zoom-fill-selected "Fill selected"]
               :icon "focus"
               :enabled [::document.subs/entities?]
               :event [::frame.events/focus-selection :fill]
               :shortcuts [{:keyCode (utils.key/codes "THREE")}]}])

(rf/dispatch [::action.events/register-action-group
              {:id :zoom/in-out
               :label [::zoom-in-out "Zoom in/out"]
               :actions [:zoom/in
                         :zoom/out]}])

(rf/dispatch [::action.events/register-action-group
              {:id :zoom/set
               :label [::zoom-set "Set zoom"]
               :actions [:zoom/set-50
                         :zoom/set-100
                         :zoom/set-200]}])

(rf/dispatch [::action.events/register-action-group
              {:id :zoom/auto
               :label [::zoom-auto "Auto zoom"]
               :actions [:zoom/focus-selected
                         :zoom/fit-selected
                         :zoom/fill-selected]}])
