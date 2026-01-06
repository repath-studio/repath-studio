(ns renderer.app.effects
  (:require
   ["@capacitor/core" :refer [Capacitor]]
   ["@capacitor/splash-screen" :refer [SplashScreen]]
   ["localforage" :as localforage]
   ["paper" :refer [paper]]
   ["sonner" :refer [toast]]
   [cognitect.transit :as transit]
   [config :as config]
   [goog.functions]
   [re-frame.core :as rf]
   [re-frame.db :as rf.db]
   [renderer.app.db :as app.db]
   [renderer.app.events :as-alias app.events]
   [renderer.history.handlers :as history.handlers]))

(rf/reg-cofx
 ::platform
 (fn [coeffects _]
   (assoc coeffects :platform (or (some-> js/window.api (.-platform))
                                  (.getPlatform Capacitor)))))

(rf/reg-cofx
 ::versions
 (fn [coeffects _]
   (cond-> coeffects
     js/window.api
     (assoc :versions (js->clj js/window.api.versions)))))

(rf/reg-cofx
 ::env
 (fn [coeffects _]
   (cond-> coeffects
     js/window.api
     (assoc :env (js->clj js/window.api.env)))))

(rf/reg-cofx
 ::user-agent
 (fn [coeffects _]
   (assoc coeffects :user-agent (.-userAgent js/navigator))))

(rf/reg-cofx
 ::standalone
 (fn [coeffects _]
   (assoc coeffects
          :standalone
          (or (.-standalone js/navigator)
              (.-matches (js/matchMedia "(display-mode: standalone"))))))

(rf/reg-cofx
 ::features
 (fn [coeffects _]
   (assoc coeffects :features
          (cond-> #{}
            (.-showSaveFilePicker js/window)
            (conj :file-system)

            (.-queryLocalFonts js/window)
            (conj :local-fonts)

            (.-EyeDropper js/window)
            (conj :eye-dropper)

            (or (.-ontouchstart js/window)
                (pos? (.-maxTouchPoints js/navigator))
                (pos? (.-msMaxTouchPoints js/navigator)))
            (conj :touch)))))

(rf/reg-fx
 ::query-local-fonts
 (fn [{:keys [on-success on-error formatter]}]
   (some-> (.-queryLocalFonts js/window)
           (.call)
           (.then #(some-> on-success
                           (conj (cond-> % formatter formatter))
                           (rf/dispatch)))
           (.catch #(some-> on-error (conj %) rf/dispatch)))))

(defn- clj->json
  [data]
  (let [writer (transit/writer :json)]
    (try (transit/write writer data)
         (catch :default err
           (rf/dispatch [::app.events/toast-error err])))))

(rf/reg-fx
 ::get-local-store
 (fn [{:keys [store-key on-success on-error on-finally formatter]}]
   (-> (localforage/getItem store-key)
       (.then #(some-> on-success
                       (conj (cond-> % formatter formatter))
                       rf/dispatch))
       (.catch #(some-> on-error (conj %) rf/dispatch))
       (.finally #(some-> on-finally rf/dispatch)))))

(rf/reg-fx
 ::set-local-store
 (fn [{:keys [store-key data on-success on-error on-finally]}]
   (-> (localforage/setItem store-key data)
       (.then #(some-> on-success (conj %) rf/dispatch))
       (.catch #(some-> on-error (conj %) rf/dispatch))
       (.finally #(some-> on-finally rf/dispatch)))))

(rf/reg-fx
 ::local-store-keys
 (fn [{:keys [on-success on-error]}]
   (-> (localforage/keys)
       (.then #(some-> on-success (conj %) rf/dispatch))
       (.catch #(some-> on-error (conj %) rf/dispatch)))))

(rf/reg-fx
 ::remove-local-store
 (fn [{:keys [store-key on-error]}]
   (-> (localforage/removeItem store-key)
       (.catch #(some-> on-error (conj %) rf/dispatch)))))

(rf/reg-fx
 ::clear-local-store
 (fn [{:keys [on-error]}]
   (-> (localforage/clear)
       (.catch #(some-> on-error (conj %) rf/dispatch)))))

(defn persist
  []
  (let [persisted-db (-> @rf.db/app-db
                         (history.handlers/drop-rest)
                         (select-keys app.db/persisted-keys))]
    (when-let [json (clj->json persisted-db)]
      (-> (localforage/setItem config/app-name json)
          (.catch #(rf/dispatch [::app.events/toast-error %]))))))

(def debounced-persist (goog.functions/debounce persist 500))

(rf/reg-fx
 ::persist
 debounced-persist)

(rf/reg-fx
 ::validate
 (fn [[db event]]
   (when (not (app.db/valid? db))
     (js/console.error (str "Event: " (first event)))
     (throw (js/Error. (str "Spec check failed: " (app.db/explain db)))))))

(rf/reg-fx
 ::install
 (fn [{:keys [prompt outcomes]}]
   (.prompt prompt)
   (-> (.-userChoice prompt)
       (.then (fn [choice]
                (some-> (get outcomes (.-outcome choice))
                        (rf/dispatch)))))))

(rf/reg-fx
 ::toast
 (fn [[toast-type title options]]
   (let [options (clj->js options)]
     (case toast-type
       :success (.success toast title options)
       :error (.error toast title options)
       :warning (.warning toast title options)
       :info (.info toast title options)
       (toast title (clj->js options))))))

(rf/reg-fx
 ::hide-splash-screen
 (fn []
   (.hide SplashScreen)))

(rf/reg-fx
 ::setup-paper
 (fn []
   (.setup paper)))
