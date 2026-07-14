(ns renderer.core
  (:require
   ["@sentry/react" :as sentry]
   ["electron-log/renderer"]
   [re-frame.core :as rf]
   [re-pressed.core :as re-pressed]
   [reagent.dom.client :as ra.dom.client]
   [renderer.a11y.core]
   [renderer.action.core]
   [renderer.app.core]
   [renderer.app.events :as-alias app.events]
   [renderer.app.views :as app.views]
   [renderer.attribute.impl.core]
   [renderer.dialog.core]
   [renderer.document.core]
   [renderer.effects]
   [renderer.element.core]
   [renderer.error.core]
   [renderer.error.views :as error.views]
   [renderer.events]
   [renderer.frame.core]
   [renderer.history.core]
   [renderer.i18n.core]
   [renderer.icon.core]
   [renderer.input.core]
   [renderer.menubar.core]
   [renderer.panel.core]
   [renderer.ruler.core]
   [renderer.shell.core]
   [renderer.snap.core]
   [renderer.theme.core]
   [renderer.timeline.core]
   [renderer.tool.core]
   [renderer.tree.events]
   [renderer.window.core]
   [renderer.worker.core]

   [user]))

(def easter-egg "
██████╗░███████╗██████╗░░█████╗░████████╗██╗░░██╗
██╔══██╗██╔════╝██╔══██╗██╔══██╗╚══██╔══╝██║░░██║
██████╔╝█████╗░░██████╔╝███████║░░░██║░░░███████║
██╔══██╗██╔══╝░░██╔═══╝░██╔══██║░░░██║░░░██╔══██║
██║░░██║███████╗██║░░░░░██║░░██║░░░██║░░░██║░░██║
╚═╝░░╚═╝╚══════╝╚═╝░░░░░╚═╝░░╚═╝░░░╚═╝░░░╚═╝░░╚═╝

░██████╗████████╗██╗░░░██╗██████╗░██╗░█████╗░
██╔════╝╚══██╔══╝██║░░░██║██╔══██╗██║██╔══██╗
╚█████╗░░░░██║░░░██║░░░██║██║░░██║██║██║░░██║
░╚═══██╗░░░██║░░░██║░░░██║██║░░██║██║██║░░██║
██████╔╝░░░██║░░░╚██████╔╝██████╔╝██║╚█████╔╝
╚═════╝░░░░╚═╝░░░░╚═════╝░╚═════╝░╚═╝░╚════╝░")

(def root-options
  #js {:onCaughtError (.reactErrorHandler sentry)
       :onRecoverableError (.reactErrorHandler sentry)
       :onUncaughtError (.reactErrorHandler
                         sentry
                         (fn [error info]
                           (js/console.warn "Uncaught error:"
                                            error (.-componentStack info))))})

(defonce root (delay (-> (.getElementById js/document "app")
                         (ra.dom.client/create-root root-options))))

(defn ^:dev/after-load mount-root! []
  (rf/clear-subscription-cache!)
  (ra.dom.client/render @root [error.views/boundary [app.views/root]]))

(defn ^:export init! []
  (rf/dispatch-sync [::app.events/initialize])
  (rf/dispatch [::re-pressed/add-keyboard-event-listener "keydown"])

  (mount-root!)
  (js/console.log easter-egg))
