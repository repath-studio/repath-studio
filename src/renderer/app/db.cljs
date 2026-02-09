(ns renderer.app.db
  (:require
   [config :as config]
   [malli.core :as m]
   [malli.transform :as m.transform]
   [renderer.a11y.db :as a11y.db :refer [A11y]]
   [renderer.db :refer [BBox Vec2 JS_Object]]
   [renderer.dialog.db :refer [Dialog]]
   [renderer.document.db :refer [Document DocumentId RecentDocument]]
   [renderer.element.db :refer [Element]]
   [renderer.event.db :refer [PointerEvent]]
   [renderer.frame.db :refer [DomRect]]
   [renderer.i18n.db
    :as i18n.db
    :refer [LanguageCodeIdentifier LanguageId Languages]]
   [renderer.icon.db :refer [Icons]]
   [renderer.icon.defaults :as icon.defaults]
   [renderer.menubar.db :refer [Menubar]]
   [renderer.panel.db :as panel.db :refer [Panel PanelId]]
   [renderer.ruler.db :refer [Ruler]]
   [renderer.snap.db :refer [Snap NearestNeighbor]]
   [renderer.theme.db :refer [Theme]]
   [renderer.timeline.db :refer [Timeline]]
   [renderer.tool.db :refer [Handle Tool State Cursor]]
   [renderer.window.db :refer [Window]]))

(def Platform
  [:enum "darwin" "linux" "win32" "ios" "android" "web"])

(def Font
  [:map-of {:title "style"} string? [:map
                                     [:postscript-name string?]
                                     [:full-name string?]]])

(def Feature
  [:enum :file-system :local-fonts :eye-dropper :touch])

(def SystemFonts
  [:map-of {:title "name"} string? Font])

(def App
  [:map {:closed true}
   [:tool {:default :transform} Tool]
   [:cached-tool {:optional true} Tool]
   [:active-pointers {:default {}} [:map-of number? PointerEvent]]
   [:pointer-pos {:default [0 0]} Vec2]
   [:pointer-offset {:optional true} Vec2]
   [:adjusted-pointer-pos {:default [0 0]} Vec2]
   [:adjusted-pointer-offset {:optional true} Vec2]
   [:nearest-neighbor-offset {:optional true} [:maybe Vec2]]
   [:nearest-neighbor {:optional true} [:maybe NearestNeighbor]]
   [:nearest-neighbors {:optional true} [:sequential NearestNeighbor]]
   [:drag-pointer {:optional true} number?]
   [:pinch-distance {:optional true} number?]
   [:pinch-midpoint {:optional true} Vec2]
   [:zoom-sensitivity {:default 0.9} [:and number? [:>= 0.01] [:<= 0.99]]]
   [:event-timestamp {:optional true} number?]
   [:features {:optional true} [:set Feature]]
   [:double-click-delta {:default 250} [:and number? pos?]]
   [:state {:default :idle} State]
   [:cached-state {:optional true} State]
   [:grid {:default false
           :persist true} boolean?]
   [:ruler {:default {}
            :persist true} Ruler]
   [:snap {:default {}
           :persist true} Snap]
   [:active-document {:optional true
                      :persist true} [:maybe DocumentId]]
   [:cursor {:default "default"} Cursor]
   [:dom-rect {:optional true} DomRect]
   [:dialogs {:default []} [:vector Dialog]]
   [:documents {:default {}
                :persist true} [:map-of DocumentId Document]]
   [:document-tabs {:default []
                    :persist true} [:vector DocumentId]]
   [:recent {:max 10
             :default []
             :persist true} [:vector RecentDocument]]
   [:drag-threshold {:default 1} number?]
   [:system-fonts {:optional true} SystemFonts]
   [:debug-info {:default false
                 :persist true} boolean?]
   [:help-bar {:default true} boolean?]
   [:loading {:default true} boolean?]
   [:pen-mode {:default false} boolean?]
   [:backdrop {:default false} boolean?]
   [:user-lang {:default "system"
                :persist true} LanguageId]
   [:system-lang {:optional true} LanguageCodeIdentifier]
   [:languages {:default i18n.db/default} Languages]
   [:icons {:default icon.defaults/icons} Icons]
   [:platform {:optional true} Platform]
   [:versions {:optional true} [:maybe map?]]
   [:env {:optional true} [:maybe map?]]
   [:standalone {:optional true} boolean?]
   [:menubar {:default {}} Menubar]
   [:install-prompt {:optional true} JS_Object]
   [:user-agent {:optional true} string?]
   [:repl-mode {:default :cljs} keyword?]
   [:error-reporting {:optional true
                      :persist true} boolean?]
   [:worker {:default {:tasks {}}} [:map [:tasks map?]]]
   [:window {:default {}} Window]
   [:theme {:default {}
            :persist true} Theme]
   [:timeline {:default {}} Timeline]
   [:panels {:default panel.db/default
             :persist true} [:map-of PanelId Panel]]
   [:version {:optional true
              :persist true} string?]
   [:fx {:default []} vector?]
   [:pivot-point {:optional true} Vec2]
   [:clicked-element {:optional true} [:or Element Handle]]
   [:clipboard {:default {}} [:map {:closed true}
                              [:bbox {:optional true} BBox]
                              [:elements {:optional true} [:* Element]]]]
   [:kdtree {:optional true} [:maybe map?]]
   [:viewbox-kdtree {:optional true} [:maybe map?]]
   [:a11y {:default {:filters a11y.db/default-filters}} A11y]
   [:re-pressed.core/keydown {:optional true} map?]])

(def valid? (m/validator App))

(def explain (m/explainer App))

(def default (m/decode App
                       {:version config/version}
                       m.transform/default-value-transformer))

(def persisted-keys
  "Top level keys that should be persisted to local storage."
  (->> App
       (m/children)
       (filter (comp :persist second))
       (map first)))
