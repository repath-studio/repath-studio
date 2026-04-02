(ns renderer.core
  (:require
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
   [renderer.event.core]
   [renderer.events]
   [renderer.frame.core]
   [renderer.history.core]
   [renderer.i18n.core]
   [renderer.icon.core]
   [renderer.menubar.core]
   [renderer.panel.core]
   [renderer.reepl.replumb :as replumb]
   [renderer.ruler.core]
   [renderer.snap.core]
   [renderer.theme.core]
   [renderer.timeline.core]
   [renderer.tool.core]
   [renderer.tree.events]
   [renderer.window.core]
   [renderer.worker.core]
   [replumb.repl :as replumb.repl]
   [shadow.cljs.bootstrap.browser :as bootstrap]
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

(defonce root (delay (-> (.getElementById js/document "app")
                         (ra.dom.client/create-root))))

(defn ^:dev/after-load mount-root! []
  (rf/clear-subscription-cache!)
  (ra.dom.client/render @root [error.views/boundary [app.views/root]]))

(defn bootstrap-cb! []
  (replumb/run-repl "(in-ns 'user)" identity)
  (print "Welcome to your REPL!")
  (print "")
  (print "You can create or modify shapes using the command line.")
  (print "Type (help) to see a list of commands."))

(defn ^:export init! []
  ;; https://code.thheller.com/blog/shadow-cljs/2017/10/14/bootstrap-support.html
  (bootstrap/init replumb.repl/st {:path "js/bootstrap"
                                   :load-on-init '[user]} bootstrap-cb!)

  (rf/dispatch-sync [::app.events/initialize])
  (rf/dispatch [::re-pressed/add-keyboard-event-listener "keydown"])

  (mount-root!)
  (js/console.log easter-egg))
