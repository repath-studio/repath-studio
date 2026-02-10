(ns core-test
  (:require
   [fixtures]
   [malli.instrument :as m.instrument]
   [re-frame.core :as rf]
   [re-frame.subs :as rf.subs]
   [renderer.a11y.events]
   [renderer.a11y.subs]
   [renderer.app.events :as app.events]
   [renderer.app.subs]
   [renderer.dialog.events]
   [renderer.document.events]
   [renderer.document.subs]
   [renderer.element.events]
   [renderer.element.impl.core]
   [renderer.element.subs]
   [renderer.error.events]
   [renderer.frame.events]
   [renderer.frame.subs]
   [renderer.history.events]
   [renderer.history.subs]
   [renderer.i18n.events]
   [renderer.i18n.subs]
   [renderer.icon.events]
   [renderer.icon.subs]
   [renderer.panel.events]
   [renderer.panel.subs]
   [renderer.ruler.events]
   [renderer.snap.events]
   [renderer.theme.events]
   [renderer.theme.subs]
   [renderer.timeline.events]
   [renderer.tool.events]
   [renderer.tool.impl.core]
   [renderer.tool.subs]
   [renderer.tree.events]
   [renderer.window.events]
   [renderer.window.subs]
   [renderer.worker.events]))

(set! rf.subs/warn-when-not-reactive (constantly nil))

(defn ^:dev/after-load instrument! []
  (m.instrument/instrument!)
  (rf/reg-global-interceptor app.events/schema-validator))

(instrument!)
