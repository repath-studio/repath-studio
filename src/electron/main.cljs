(ns electron.main
  "https://www.electronjs.org/docs/latest/tutorial/process-model#the-main-process"
  (:require
   ["@electron/devtron" :refer [devtron]]
   ["@sentry/electron/main" :as sentry-electron-main]
   ["electron" :refer [app shell ipcMain BrowserWindow]]
   ["electron-log/main" :as log]
   ["electron-updater" :refer [autoUpdater]]
   ["electron-window-state" :as window-state-keeper]
   ["os" :as os]
   ["path" :as path]
   ["url" :as url]
   [config :as config]
   [electron.file :as file]))

(defonce main-window (atom nil))
(defonce loading-window (atom nil))

(defn send-to-renderer
  ([channel]
   (send-to-renderer channel nil))
  ([channel data]
   (.send (.-webContents ^js @main-window) channel (clj->js data))))

(def allowed-urls
  #{"repath.studio"
    "github.com"
    "developer.mozilla.org"
    "svgwg.org"
    "fxtf.org"
    "drafts.fxtf.org"
    "csswg.org"
    "drafts.csswg.org"
    "blobs.dev"})

(defn allowed-url?
  [^js/Url url]
  (and (= (.-protocol url) "https:")
       (contains? allowed-urls (.-host url))))

(defn open-external
  [url]
  (let [url-parsed (js/URL. url)]
    (when (allowed-url? url-parsed)
      (.openExternal shell url-parsed.href))))

(defn register-ipc-on-events []
  (doseq
   [[e f]
    [["initialized" #(.close ^js @loading-window)]
     ["relaunch" #(doto app (.relaunch) (.exit))]
     ["open-remote-url" open-external]
     ["open-directory" #(.showItemInFolder shell %)]
     ["window-minimize" #(.minimize ^js @main-window)]
     ["window-toggle-fullscreen" #(.setFullScreen ^js @main-window
                                                  (not (.isFullScreen
                                                        ^js @main-window)))]
     ["window-toggle-maximized" #(if (.isMaximized ^js @main-window)
                                   (.unmaximize ^js @main-window)
                                   (.maximize ^js @main-window))]]]
    (.on ipcMain e #(f %2))))

(defn register-ipc-handle-events []
  (doseq
   [[e f]
    [["open-documents" file/open]
     ["save-document" file/save]
     ["save-document-as" file/save-as]
     ["print" file/show-print-dialog]]]
    (.handle ipcMain e #(f %2))))

(defn register-window-events []
  (doseq
   [[window-event renderer-event]
    [["maximize" "window-maximized"]
     ["unmaximize" "window-unmaximized"]
     ["enter-full-screen" "window-entered-fullscreen"]
     ["leave-full-screen" "window-leaved-fullscreen"]
     ["minimize" "window-minimized"]
     ["restore" "window-restored"]]]
    (.on ^js @main-window window-event #(send-to-renderer renderer-event))))

(defn register-web-contents-events []
  (let [web-contents (.-webContents ^js @main-window)]
    (doseq
     [[web-contents-event f]
      [["will-frame-navigate" #(.preventDefault %)]
       ["closed" #(reset! main-window nil)]]]
      (.on web-contents web-contents-event f))))

(defn set-window-open-handler []
  (.setWindowOpenHandler (.-webContents ^js @main-window)
                         (fn [details]
                           (open-external (.-url details))
                           #js {:action "deny"})))

(defn on-ready-to-show
  [^js window]
  (send-to-renderer (if (.isMaximized window)
                      "window-maximized"
                      "window-unmaximized"))
  (send-to-renderer (if (.isFullScreen window)
                      "window-entered-fullscreen"
                      "window-leaved-fullscreen")))

(defn resource-path
  [s]
  (url/format #js {:pathname (.join path js/__dirname s)
                   :protocol "file"}))

(defn init-main-window! []
  (let [win-state (window-state-keeper #js {:defaultWidth 1920
                                            :defaultHeight 1080})]
    (reset! main-window
            (BrowserWindow.
             #js {:x (.-x win-state)
                  :y (.-y win-state)
                  :width (.-width win-state)
                  :height (.-height win-state)
                  :titleBarStyle (when (= (.platform os) "darwin") "hidden")
                  :trafficLightPosition #js {:x 8
                                             :y 10}
                  :icon (resource-path "/public/img/icon.png")
                  :frame false
                  :show false
                  :webPreferences
                  #js {:sandbox false
                       :preload (.join path js/__dirname "preload.js")}}))

    (when config/debug?
      (.install devtron))

    (.once ^js @main-window
           "ready-to-show"
           (fn []
             (.show ^js @main-window)
             (.manage win-state ^js @main-window)
             ;; Fixes a bug on linux that blocks unmaximize after load.
             (.restore ^js @main-window)
             (.initialize log)))

    (.on ^js @main-window "ready-to-show" #(on-ready-to-show @main-window))

    (.loadURL ^js @main-window (if config/debug?
                                 "http://localhost:8080"
                                 (resource-path "/public/index.html")))

    (set-window-open-handler)
    (register-web-contents-events)
    (register-ipc-on-events)
    (register-ipc-handle-events)
    (register-window-events)

    (.checkForUpdatesAndNotify autoUpdater)))

(defn init-loading-window! []
  (set! (.-allowRendererProcessReuse app) false)
  (reset! loading-window
          (BrowserWindow.
           #js {:width 720
                :height 576
                :icon (resource-path "/public/img/icon.png")
                :show false
                :alwaysOnTop true
                :transparent true
                :frame false}))
  (.once ^js @loading-window "show" init-main-window!)
  (.loadURL ^js @loading-window (resource-path "/public/loading.html"))
  (.once ^js (.-webContents @loading-window)
         "did-finish-load"
         #(.show ^js @loading-window)))

(defn ^:export init! []
  (sentry-electron-main/init config/sentry)
  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                  (.quit app)))
  (.on app "ready" init-loading-window!))
