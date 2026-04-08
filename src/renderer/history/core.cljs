(ns renderer.history.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.dialog.events :as-alias dialog.events]
   [renderer.history.events :as history.events]
   [renderer.history.subs :as history.subs]
   [renderer.utils.key :as utils.key]))

(rf/dispatch [::action.events/register-action
              {:id :history/undo
               :label [::undo "Undo"]
               :icon "undo"
               :event [::history.events/undo]
               :shortcuts [{:keyCode (utils.key/codes "Z")
                            :ctrlKey true}]
               :enabled [::history.subs/undos?]}])

(rf/dispatch [::action.events/register-action
              {:id :history/redo
               :label [::redo "Redo"]
               :icon "redo"
               :event [::history.events/redo]
               :shortcuts [{:keyCode (utils.key/codes "Z")
                            :ctrlKey true
                            :shiftKey true}
                           {:keyCode (utils.key/codes "Y")
                            :ctrlKey true}]
               :enabled [::history.subs/redos?]}])

(rf/dispatch [::action.events/register-action
              {:id :history/clear
               :label [::clear-history "Clear history"]
               :icon "delete"
               :event [::dialog.events/confirm-irreversible-action
                       {:confirm-event [::history.events/clear]
                        :confirm-label [::clear-history "Clear history"]}]}])

(rf/dispatch [::action.events/register-action-group
              {:id :edit/history
               :label [::history "History"]
               :actions [:history/undo
                         :history/redo]}])
