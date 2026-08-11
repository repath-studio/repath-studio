(ns renderer.menubar.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.events :as-alias events]
   [renderer.menubar.events :as menubar.events]
   [renderer.menubar.subs]
   [renderer.utils.key :as utils.key]
   [renderer.window.subs :as-alias window.subs]))

(rf/dispatch [::action.events/register-action
              {:id :menubar/activate-file
               :label [::menubar-file "File menu"]
               :icon "file"
               :event [::menubar.events/activate :file]
               :available [::window.subs/md?]
               :shortcuts [{:keyCode (utils.key/codes "F")
                            :altKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :menubar/activate-edit
               :label [::menubar-edit "Edit menu"]
               :icon "pencil"
               :event [::menubar.events/activate :edit]
               :available [::window.subs/md?]
               :shortcuts [{:keyCode (utils.key/codes "E")
                            :altKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :menubar/activate-object
               :label [::menubar-object "Object menu"]
               :icon "circle"
               :event [::menubar.events/activate :object]
               :available [::window.subs/md?]
               :shortcuts [{:keyCode (utils.key/codes "O")
                            :altKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :menubar/activate-view
               :label [::menubar-view "View menu"]
               :icon "eye"
               :event [::menubar.events/activate :view]
               :available [::window.subs/md?]
               :shortcuts [{:keyCode (utils.key/codes "V")
                            :altKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :menubar/activate-help
               :label [::menubar-help "Help menu"]
               :icon "help"
               :event [::menubar.events/activate :help]
               :available [::window.subs/md?]
               :shortcuts [{:keyCode (utils.key/codes "H")
                            :altKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :help/website
               :label [::website "Website"]
               :icon "earth"
               :event [::events/open-remote-url
                       "https://repath.studio/"]}])

(rf/dispatch [::action.events/register-action
              {:id :help/source-code
               :label [::source-code "Source Code"]
               :icon "commit"
               :event [::events/open-remote-url
                       "https://github.com/repath-studio/repath-studio"]}])

(rf/dispatch [::action.events/register-action
              {:id :help/license
               :label [::license "License"]
               :icon "lgpl"
               :event [::events/open-remote-url
                       "https://repath.studio/policies/license/"]}])

(rf/dispatch [::action.events/register-action
              {:id :help/changelog
               :label [::changelog "Changelog"]
               :icon "list"
               :event [::events/open-remote-url
                       "https://repath.studio/roadmap/changelog/"]}])

(rf/dispatch [::action.events/register-action
              {:id :help/privacy-policy
               :label [::privacy-policy "Privacy Policy"]
               :icon "lock"
               :event [::events/open-remote-url
                       "https://repath.studio/policies/privacy/"]}])

(rf/dispatch [::action.events/register-action
              {:id :help/submit-issue
               :label [::submit-an-issue "Submit an issue"]
               :icon "warning"
               :event [::events/open-remote-url
                       "https://github.com/repath-studio/repath-studio/issues/new/choose"]}])

(rf/dispatch [::action.events/register-action-group
              {:id :help-links
               :icon "help"
               :label [::help-links "Help links"]
               :actions [:help/website
                         :help/source-code
                         :help/license
                         :help/changelog
                         :help/privacy-policy
                         :help/submit-issue]}])

(rf/dispatch [::action.events/register-action-group
              {:id :menubar
               :icon "menu"
               :label [::menubar "Menu bar"]
               :actions [:menubar/activate-file
                         :menubar/activate-edit
                         :menubar/activate-object
                         :menubar/activate-view
                         :menubar/activate-help]}])
