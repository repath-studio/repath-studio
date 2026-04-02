(ns fixtures
  (:require
   ["paper" :refer [paper]]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.action.effects :as-alias action.effects]
   [renderer.app.db :as app.db]
   [renderer.app.effects :as-alias app.effects]
   [renderer.effects :as-alias effects]
   [renderer.element.effects :as-alias element.effects]
   [renderer.error.effects :as-alias error.effects]
   [renderer.event.events :as-alias event.events]
   [renderer.i18n.effects :as-alias i18n.effects]
   [renderer.theme.effects :as-alias theme.effects]
   [renderer.utils.element :as utils.element]
   [renderer.window.effects :as-alias window.effects]))

(defonce local-store
  (atom {config/app-name
         {:error-reporting false
          :recent [{:id #uuid "123e4567-e89b-12d3-a456-426614174000"
                    :title "drawing-1.rps"}]}}))

(rf/reg-fx
 ::effects/add-event-listener
 (fn [_]))

(rf/reg-fx
 ::app.effects/get-local-store
 (fn [{:keys [store-key on-success on-finally]}]
   (some-> on-success (conj (get @local-store store-key)) rf/dispatch)
   (some-> on-finally rf/dispatch)))

(rf/reg-fx
 ::app.effects/set-local-store
 (fn [{:keys [store-key data on-success]}]
   (swap! local-store assoc store-key data)
   (some-> on-success (conj data) rf/dispatch)))

(rf/reg-fx
 ::app.effects/local-store-keys
 (fn [{:keys [on-success]}]
   (some-> on-success (conj (keys @local-store)) rf/dispatch)))

(rf/reg-fx
 ::app.effects/remove-local-store
 (fn [{:keys [store-key]}]
   (swap! local-store dissoc store-key)))

(rf/reg-fx
 ::app.effects/validate
 (fn [[db event]]
   (when (and (not (app.db/valid? db)) (not= db {}))
     (js/console.error (str "Event: " (first event)))
     (throw (js/Error. (str "Spec check failed: " (app.db/explain db)))))))

(rf/reg-cofx
 ::app.effects/platform
 (fn [coeffects _]
   (assoc coeffects :platform "web")))

(rf/reg-cofx
 ::app.effects/versions
 (fn [coeffects _]
   coeffects))

(rf/reg-cofx
 ::app.effects/env
 (fn [coeffects _]
   coeffects))

(rf/reg-fx
 ::app.effects/persist
 (fn [coeffects _]
   coeffects))

(rf/reg-cofx
 ::window.effects/fullscreen
 (fn [coeffects _]
   (assoc coeffects :fullscreen true)))

(rf/reg-cofx
 ::window.effects/focused
 (fn [coeffects _]
   (assoc coeffects :focused false)))

(rf/reg-cofx
 ::window.effects/width
 (fn [coeffects _]
   (assoc coeffects :width 800)))

(rf/reg-cofx
 ::i18n.effects/language
 (fn [coeffects _]
   (assoc coeffects :language "en-US")))

(rf/reg-cofx
 ::theme.effects/theme-color
 (fn [coeffects _]
   (assoc coeffects :theme-color "#ffffff")))

(rf/reg-cofx
 ::theme.effects/native-mode
 (fn [coeffects _]
   (assoc coeffects :native-mode :light)))

(rf/reg-fx
 ::theme.effects/add-listener
 (fn [_]))

(rf/reg-fx
 ::theme.effects/set-status-bar-style
 (fn [_]))

(rf/reg-fx
 ::error.effects/init-reporting
 (fn [_]))

(rf/reg-fx
 ::app.effects/query-local-fonts
 (fn [{:keys [on-success formatter]}]
   (let [font-data (clj->js [{:family "Noto Sans"
                              :fullName "Noto Sans Regular"
                              :postscriptName "Noto-Sans-Regular"
                              :style "Regular"}
                             {:family "Adwaita Mono"
                              :fullName "Adwaita Mono Bold"
                              :postscriptName "Adwaita-Mono-Bold"
                              :style "Bold"}
                             {:family "Adwaita Mono"
                              :fullName "Adwaita Mono Bold Italic"
                              :postscriptName "Adwaita-Mono-Bold-Italic"
                              :style "Bold Italic"}])]
     (rf/dispatch (conj on-success (cond-> font-data
                                     formatter formatter))))))

(rf/reg-cofx
 ::app.effects/features
 (fn [coeffects _]
   (assoc coeffects :features #{:file-system :local-fonts})))

(rf/reg-cofx
 ::app.effects/user-agent
 (fn [coeffects _]
   (assoc coeffects
          :user-agent
          "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36
           (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36")))

(rf/reg-cofx
 ::app.effects/standalone
 (fn [coeffects _]
   (assoc coeffects :standalone false)))

(rf/reg-fx
 ::app.effects/setup-paper
 (fn [_]
   (.setup paper)))

(rf/reg-fx
 ::app.effects/hide-splash-screen
 (fn [_]))

(rf/reg-fx
 ::element.effects/->path
 (fn [{:keys [data on-success on-error]}]
   (-> (mapv utils.element/->path data)
       (js/Promise.all)
       (.then #(some-> on-success (conj %) rf/dispatch))
       (.catch #(some-> on-error (conj %) rf/dispatch)))))

(rf/reg-cofx
 ::effects/guid
 (fn [coeffects _]
   (assoc coeffects :guid (random-uuid))))

(rf/reg-fx
 ::effects/ipc-on
 (fn [_]))

(rf/reg-fx
 ::effects/ipc-send
 (fn [_]))

(rf/reg-fx
 ::effects/set-document-attr
 (fn [_]))

(rf/reg-fx
 ::effects/set-meta
 (fn [_]))

(rf/reg-fx
 ::action.effects/update-keydown-rules
 (fn [_]))

(rf/reg-event-db
 ::event.events/keyboard
 (fn [_]))
