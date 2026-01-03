(ns renderer.app.events
  (:require
   [cognitect.transit :as transit]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.db :as app.db]
   [renderer.app.effects :as-alias app.effects]
   [renderer.app.events :as-alias app.events]
   [renderer.document.events :as-alias document.events]
   [renderer.document.handlers :as document.handlers]
   [renderer.effects :as-alias effects]
   [renderer.error.events :as-alias error.events]
   [renderer.event.events :as-alias event.events]
   [renderer.event.impl.keyboard :as impl.keyboard]
   [renderer.history.handlers :as history.handlers]
   [renderer.i18n.effects :as-alias i18n.effects]
   [renderer.i18n.events :as-alias i18n.events]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.theme.effects :as-alias theme.effects]
   [renderer.theme.events :as-alias theme.events]
   [renderer.utils.font :as utils.font]
   [renderer.window.events :as-alias window.events]))

(def persist
  (rf/->interceptor
   :id ::persist
   :after (fn [context]
            (let [db (rf/get-effect context :db)
                  fx (rf/get-effect context :fx)]
              (cond-> context
                db
                (rf/assoc-effect :fx (conj (or fx [])
                                           [::app.effects/persist])))))))

(def ipc-listeners
  (->> [["window-maximized" [::window.events/set-maximized true]]
        ["window-unmaximized" [::window.events/set-maximized false]]
        ["window-entered-fullscreen" [::window.events/set-fullscreen true]]
        ["window-leaved-fullscreen" [::window.events/set-fullscreen false]]
        ["window-minimized" [::window.events/set-minimized true]]
        ["window-restored" [::window.events/set-minimized false]]]
       (mapv (partial vector ::effects/ipc-on))))

(defn- json->clj
  [data]
  (let [reader (transit/reader :json)]
    (try (transit/read reader data)
         (catch :default err
           (rf/dispatch [::app.events/toast-error err])))))

(rf/reg-event-fx
 ::initialize
 [(rf/inject-cofx ::app.effects/user-agent)
  (rf/inject-cofx ::app.effects/platform)
  (rf/inject-cofx ::app.effects/versions)
  (rf/inject-cofx ::app.effects/env)
  (rf/inject-cofx ::app.effects/standalone)
  (rf/inject-cofx ::app.effects/features)
  (rf/inject-cofx ::i18n.effects/language)]
 (fn [{:keys [user-agent platform versions env standalone features language]} _]
   {:db (assoc app.db/default
               :platform platform
               :versions versions
               :env env
               :standalone standalone
               :user-agent user-agent
               :features features
               :system-lang language)
    :fx (into [[::app.effects/get-local-store
                {:store-key config/app-name
                 :formatter json->clj
                 :on-success [::load-local-db]
                 :on-error [::app.events/toast-error]
                 :on-finally [::db-loaded]}]]
              ipc-listeners)}))

(rf/reg-event-fx
 ::load-local-db
 (fn [{:keys [db]} [_ persisted-db]]
   (let [app-db (merge db persisted-db)]
     (if (app.db/valid? app-db)
       {:db app-db}
       {::app.effects/clear-local-store nil}))))

(rf/reg-event-db
 ::set-loading
 (fn [db [_ state]]
   (assoc db :loading state)))

(rf/reg-event-db
 ::set-install-prompt
 (fn [db [_ prompt]]
   (assoc db :install-prompt prompt)))

(rf/reg-event-fx
 ::install
 (fn [{:keys [db]} _]
   (when-let [install-prompt (:install-prompt db)]
     {::app.effects/install
      {:prompt install-prompt
       :outcomes {"dismissed" nil
                  "accepted" [::set-install-prompt nil]}}})))

(def listeners
  (->> [[js/document "keydown" [::event.events/keyboard] impl.keyboard/->clj]
        [js/document "keyup" [::event.events/keyboard] impl.keyboard/->clj]
        [js/document "fullscreenchange" [::window.events/update-fullscreen]]
        [js/window "focus" [::window.events/update-focused]]
        [js/window "blur" [::window.events/update-focused]]
        [js/window "resize" [::window.events/update-width]]
        [js/window "beforeinstallprompt" [::set-install-prompt]]]
       (mapv (partial vector ::effects/add-event-listener))))

(rf/reg-event-fx
 ::db-loaded
 [(rf/inject-cofx ::effects/guid)]
 (fn [{:keys [db guid]} _]
   {:db (if (:active-document db)
          (snap.handlers/rebuild-tree db)
          (-> db
              (document.handlers/create guid)
              (history.handlers/finalize [::create-doc "Create document"])))
    ;; The order of the following events is important.
    ;; Changes require thorough testing on all platforms.
    :fx (into [[:dispatch [::error.events/init-reporting]]
               ;; We initialize values that might flicker the view first.
               [:dispatch [::theme.events/set-native-mode]]
               [:dispatch [::theme.events/update-data-theme]]
               [:dispatch [::theme.events/update-meta-color]]
               [:dispatch [::window.events/update-width]]
               [:dispatch [::i18n.events/set-lang-attrs]]
               [:dispatch [::set-loading false]]
               [::app.effects/hide-splash-screen]
               ;; We flush to render once so we can get the canvas size.
               [:dispatch ^:flush-dom [::document.events/center]]
               [:dispatch [::window.events/update-focused]]
               [::effects/ipc-send ["initialized"]]
               [::theme.effects/add-listener [::theme.events/set-native-mode]]
               [::app.effects/setup-paper]
               ;; The status bar needs to be updated later for some reason.
               [:dispatch [::theme.events/update-status-bar]]]
              listeners)}))

(rf/reg-event-db
 ::set-system-fonts
 (fn [db [_ fonts]]
   (assoc db :system-fonts fonts)))

(rf/reg-event-db
 ::set-repl-mode
 (fn [db [_ mode]]
   (assoc db :repl-mode mode)))

(rf/reg-event-db
 ::toggle-debug-info
 (fn [db [_]]
   (update db :debug-info not)))

(rf/reg-event-db
 ::toggle-help-bar
 (fn [db [_]]
   (update db :help-bar not)))

(rf/reg-event-db
 ::set-backdrop
 (fn [db [_ visible]]
   (assoc db :backdrop visible)))

(rf/reg-event-db
 ::toggle-grid
 [persist]
 (fn [db [_]]
   (update db :grid not)))

(rf/reg-event-fx
 ::load-system-fonts
 (fn [_ _]
   {::app.effects/query-local-fonts
    {:on-success [::set-system-fonts]
     :on-error [::app.events/toast-error]
     :formatter utils.font/font-data->system-fonts}}))

(def schema-validator
  (rf/->interceptor
   :id ::schema-validator
   :after (fn [context]
            (let [db (or (rf/get-effect context :db)
                         (rf/get-coeffect context :db))
                  fx (rf/get-effect context :fx)
                  event (rf/get-coeffect context :event)]
              (cond-> context
                db
                (rf/assoc-effect :fx
                                 (conj (or fx [])
                                       [::app.effects/validate
                                        [db event]])))))))

(rf/reg-event-fx
 ::toast
 (fn [_ [_ toast-type title options]]
   {::app.effects/toast [toast-type title options]}))

(rf/reg-event-fx
 ::toast-error
 (fn [_ [_ ^js/Error error]]
   {::app.effects/toast [:error
                         (.-name error)
                         {:description (or (.-message error)
                                           (str error))}]}))
