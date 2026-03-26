(ns renderer.core
  (:require
   ["electron-log/renderer"]
   [re-frame.core :as rf]
   [re-pressed.core :as re-pressed]
   [reagent.dom.client :as ra.dom.client]
   [renderer.a11y.events]
   [renderer.a11y.subs]
   [renderer.action.defaults :as action.defaults]
   [renderer.action.subs]
   [renderer.app.effects]
   [renderer.app.events :as app.events]
   [renderer.app.subs]
   [renderer.app.views :as app.views]
   [renderer.attribute.impl.core]
   [renderer.dialog.events]
   [renderer.dialog.subs]
   [renderer.document.effects]
   [renderer.document.events]
   [renderer.document.subs]
   [renderer.effects]
   [renderer.element.effects]
   [renderer.element.events]
   [renderer.element.impl.core]
   [renderer.element.subs]
   [renderer.error.effects]
   [renderer.error.events]
   [renderer.error.subs]
   [renderer.error.views :as error.views]
   [renderer.event.effects]
   [renderer.event.events]
   [renderer.events]
   [renderer.frame.events]
   [renderer.frame.subs]
   [renderer.history.events]
   [renderer.history.subs]
   [renderer.i18n.effects]
   [renderer.i18n.events]
   [renderer.i18n.subs]
   [renderer.icon.events]
   [renderer.icon.subs]
   [renderer.menubar.events]
   [renderer.menubar.subs]
   [renderer.panel.events]
   [renderer.panel.subs]
   [renderer.reepl.replumb :as replumb]
   [renderer.ruler.events]
   [renderer.ruler.subs]
   [renderer.snap.events]
   [renderer.snap.subs]
   [renderer.theme.effects]
   [renderer.theme.events]
   [renderer.theme.subs]
   [renderer.timeline.effects]
   [renderer.timeline.events]
   [renderer.timeline.subs]
   [renderer.tool.events]
   [renderer.tool.impl.core]
   [renderer.tool.subs]
   [renderer.tree.events]
   [renderer.utils.key :as utils.key]
   [renderer.window.effects]
   [renderer.window.events]
   [renderer.window.subs]
   [renderer.worker.effects]
   [renderer.worker.events]
   [renderer.worker.subs]
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
