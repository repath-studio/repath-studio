(ns renderer.error.effects
  (:require
   ["@sentry/capacitor" :as sentry-capacitor]
   ["@sentry/electron/renderer" :as sentry-electron-renderer]
   ["@sentry/react" :as sentry-react]
   [re-frame.core :as rf]
   [renderer.utils.platform :as utils.platform]))

(rf/reg-fx
 ::init-reporting
 (fn [[platform config]]
   (cond
     (utils.platform/desktop? platform)
     (sentry-electron-renderer/init config sentry-react/init)

     (utils.platform/mobile? platform)
     (sentry-capacitor/init config sentry-react/init)

     (utils.platform/web? platform)
     (sentry-react/init config))))
