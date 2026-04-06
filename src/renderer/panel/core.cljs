(ns renderer.panel.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.panel.events :as panel.events]
   [renderer.panel.subs :as panel.subs]
   [renderer.utils.key :as utils.key]))

(rf/dispatch [::action.events/register-action
              {:id :panel/toggle-tree
               :label [::panel-element-tree "Element tree"]
               :icon "tree"
               :event [::panel.events/toggle :tree]
               :active [::panel.subs/visible? :tree]
               :shortcuts [{:keyCode (utils.key/codes "T")
                            :ctrlKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :panel/toggle-properties
               :label [::panel-properties "Properties"]
               :icon "properties"
               :event [::panel.events/toggle :properties]
               :active [::panel.subs/visible? :properties]
               :shortcuts [{:keyCode (utils.key/codes "P")
                            :ctrlKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :panel/toggle-xml
               :label [::panel-xml-view "XML view"]
               :icon "code"
               :event [::panel.events/toggle :xml]
               :active [::panel.subs/visible? :xml]}])

(rf/dispatch [::action.events/register-action
              {:id :panel/toggle-history
               :label [::panel-history-tree "History tree"]
               :icon "history"
               :event [::panel.events/toggle :history]
               :active [::panel.subs/visible? :history]
               :shortcuts [{:keyCode (utils.key/codes "H")
                            :ctrlKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :panel/toggle-repl-history
               :label [::panel-shell-history "Shell history"]
               :icon "shell"
               :event [::panel.events/toggle :repl-history]
               :active [::panel.subs/visible? :repl-history]}])

(rf/dispatch [::action.events/register-action
              {:id :panel/toggle-timeline
               :label [::panel-timeline-editor "Timeline editor"]
               :icon "animation"
               :event [::panel.events/toggle :timeline]
               :active [::panel.subs/visible? :timeline]}])
