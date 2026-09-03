(ns electron.preload
  "https://www.electronjs.org/docs/latest/tutorial/tutorial-preload"
  (:require
   ["electron" :refer [contextBridge ipcRenderer]]))

(def api
  {;; Strip event as it includes `sender`.
   ;; https://www.electronjs.org/docs/latest/api/ipc-renderer#ipcrendereronchannel-listener
   :on (fn [channel f] (.on ipcRenderer channel (fn [_event args] (f args))))
   :send (fn [channel args] (.send ipcRenderer channel args))
   :invoke (fn [channel args] (.invoke ipcRenderer channel args))
   :platform (.-platform js/process)
   :versions (.-versions js/process)})

(defn ^:export init!
  "Expose protected ipcRenderer methods to the renderer process.
   https://www.electronjs.org/docs/api/context-bridge"
  []
  (.exposeInMainWorld contextBridge "api" (clj->js api)))
