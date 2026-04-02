(ns renderer.core
  (:require
   ["electron-log/renderer"]
   [re-frame.core :as rf]
   [re-pressed.core :as re-pressed]
   [reagent.dom.client :as ra.dom.client]
   [renderer.a11y.core]
   [renderer.action.core]
   [renderer.action.defaults :as action.defaults]
   [renderer.action.subs]
   [renderer.app.core]
   [renderer.app.events :as-alias app.events]
   [renderer.app.views :as app.views]
   [renderer.attribute.impl.core]
   [renderer.dialog.events]
   [renderer.dialog.subs]
   [renderer.document.core]
   [renderer.effects]
   [renderer.element.core]
   [renderer.error.core]
   [renderer.error.views :as error.views]
   [renderer.event.effects]
   [renderer.event.events]
   [renderer.events]
   [renderer.frame.events]
   [renderer.frame.subs]
   [renderer.history.events]
   [renderer.history.subs]
   [renderer.i18n.core]
   [renderer.icon.core]
   [renderer.menubar.events]
   [renderer.menubar.subs]
   [renderer.panel.events]
   [renderer.panel.subs]
   [renderer.reepl.replumb :as replumb]
   [renderer.ruler.events]
   [renderer.ruler.subs]
   [renderer.snap.events]
   [renderer.snap.subs]
   [renderer.theme.core]
   [renderer.timeline.core]
   [renderer.tool.core]
   [renderer.tree.events]
   [renderer.utils.key :as utils.key]
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
  (rf/dispatch [::re-pressed/set-keydown-rules (utils.key/actions->keydown-rules
                                                action.defaults/actions)])

  (mount-root!)
  (js/console.log easter-egg))
