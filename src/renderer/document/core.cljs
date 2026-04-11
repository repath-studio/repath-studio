(ns renderer.document.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.app.subs :as-alias app.subs]
   [renderer.dialog.events :as-alias dialog.events]
   [renderer.document.effects]
   [renderer.document.events :as document.events]
   [renderer.document.subs :as document.subs]
   [renderer.utils.key :as utils.key]))

(rf/dispatch [::action.events/register-action
              {:id :document/new
               :label [::new "New"]
               :icon "file"
               :event [::document.events/new]
               :shortcuts [{:keyCode (utils.key/codes "N")
                            :ctrlKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :document/new
               :label [::new "New"]
               :icon "file"
               :event [::document.events/new]
               :shortcuts [{:keyCode (utils.key/codes "N")
                            :ctrlKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :document/open
               :label [::open "Open…"]
               :icon "folder"
               :event [::document.events/open]
               :shortcuts [{:keyCode (utils.key/codes "O")
                            :ctrlKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :document/save
               :label [::save "Save"]
               :icon "save"
               :event [::document.events/save]
               :shortcuts [{:keyCode (utils.key/codes "S")
                            :ctrlKey true}]
               :enabled [::document.subs/saveable?]
               :available [::app.subs/supported-feature? :file-system]}])

(rf/dispatch [::action.events/register-action
              {:id :document/save-as
               :label [::save-as "Save as…"]
               :icon "save-as"
               :event [::document.events/save-as]
               :shortcuts [{:keyCode (utils.key/codes "S")
                            :ctrlKey true
                            :shiftKey true}]
               :enabled [::document.subs/entities?]
               :available [::app.subs/supported-feature? :file-system]}])

(rf/dispatch [::action.events/register-action
              {:id :document/download
               :label [::download "Download"]
               :icon "download"
               :event [::document.events/download]
               :enabled [::document.subs/entities?]
               :available [::app.subs/unsupported-feature? :file-system]}])

(rf/dispatch [::action.events/register-action
              {:id :document/close
               :label [::close "Close"]
               :icon "window-close"
               :event [::document.events/close-active]
               :shortcuts [{:keyCode (utils.key/codes "W")
                            :ctrlKey true}]
               :enabled [::document.subs/entities?]}])

(rf/dispatch [::action.events/register-action
              {:id :document/close-all
               :label [::close-all "Close all"]
               :icon "window-close"
               :event [::document.events/close-all]
               :shortcuts [{:keyCode (utils.key/codes "W")
                            :ctrlKey true
                            :altKey true}]}])

(def clear-recent-label [::recent-clear "Clear recent"])

(rf/dispatch [::action.events/register-action
              {:id :document/clear-recent
               :label clear-recent-label
               :icon "delete"
               :enabled [::document.subs/some-recent?]
               :event [::dialog.events/confirm-irreversible-action
                       {:confirm-event [::document.events/clear-recent]
                        :confirm-label clear-recent-label}]}])

(rf/dispatch [::action.events/register-action
              {:id :document/close-saved
               :label [::close-saved "Close saved"]
               :icon "window-close"
               :event [::document.events/close-saved]
               :enabled [::document.subs/some-saved?]}])

(rf/dispatch [::action.events/register-action
              {:id :document/print
               :label [::print "Print"]
               :icon "printer"
               :event [::document.events/print]
               :enabled [::document.subs/entities?]}])

(rf/dispatch [::action.events/register-action
              {:id :export/svg
               :label [::svg "SVG"]
               :icon "export"
               :event [::document.events/export "image/svg+xml"]
               :enabled [::document.subs/entities?]}])

(rf/dispatch [::action.events/register-action
              {:id :export/png
               :label [::png "PNG"]
               :icon "export"
               :event [::document.events/export "image/png"]
               :enabled [::document.subs/entities?]}])

(rf/dispatch [::action.events/register-action
              {:id :export/jpg
               :label [::jpg "JPG"]
               :icon "export"
               :event [::document.events/export "image/jpeg"]
               :enabled [::document.subs/entities?]}])

(rf/dispatch [::action.events/register-action
              {:id :export/webp
               :label [::webp "WEBP"]
               :icon "export"
               :event [::document.events/export "image/webp"]
               :enabled [::document.subs/entities?]}])

(rf/dispatch [::action.events/register-action
              {:id :export/gif
               :label [::gif "GIF"]
               :icon "export"
               :event [::document.events/export "image/gif"]
               :enabled [::document.subs/entities?]}])

(rf/dispatch [::action.events/register-action-group
              {:id :export/vector
               :label [::vector-formats "Vector formats"]
               :actions [:export/svg]}])

(rf/dispatch [::action.events/register-action-group
              {:id :export/raster
               :label [::rasterised-formats "Rasterised formats"]
               :actions [:export/png
                         :export/jpg
                         :export/webp
                         :export/gif]}])
