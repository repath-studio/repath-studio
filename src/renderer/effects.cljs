(ns renderer.effects
  (:require
   [cljs.reader :refer [read-string]]
   [clojure.string :as string]
   [config :as config]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.element.handlers :as element.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.utils.dom :as utils.dom]
   [renderer.utils.element :as utils.element]))

(rf/reg-cofx
 ::guid
 (fn [coeffects _]
   (assoc coeffects :guid (random-uuid))))

(rf/reg-cofx
 ::now
 (fn [coeffects _]
   (assoc coeffects :now (.now js/performance))))

(rf/reg-fx
 ::focus
 (fn [el-ref]
   (some-> el-ref (.focus))))

(rf/reg-fx
 ::blur
 (fn []
   (some-> (.-activeElement js/document)
           (.blur))))

(rf/reg-cofx
 ::time-origin
 (fn [coeffects _]
   (assoc coeffects :time-origin (.-timeOrigin js/performance))))

(m/=> ->payload [:-> map? string?])
(defn ->payload
  [{:keys [elements bbox]}]
  (pr-str {:bbox bbox
           :elements elements}))

(m/=> payload->data [:-> string? [:maybe map?]])
(defn payload->data
  [payload]
  (when-let [data (some-> payload read-string)]
    (when (sequential? (:elements data))
      data)))

(def custom-mime-type
  "Custom clipboard MIME-type format.
   https://developer.mozilla.org/en-US/docs/Web/API/ClipboardItem/supports_static"
  (str "web " config/mime-type))

(defn file-data-fn
  [item mime]
  (fn []
    (-> item
        (.getType mime)
        (.then (fn [^js/Blob blob]
                 (if (and (= mime "image/svg+xml") (empty? (.-name blob)))
                   (-> (.text blob) (.then element.handlers/svg->data))
                   {:file (js/File. (array blob)
                                    (or (.-name blob) "pasted")
                                    #js {:type mime})}))))))

(rf/reg-fx
 ::clipboard-write
 (fn [{:keys [data on-error]}]
   (let [svg (utils.element/->svg (:elements data))]
     (-> (let [blob-array (js-obj)]
           (doseq [[data-type data] [[custom-mime-type (->payload data)]
                                     ["image/svg+xml" svg]
                                     ["text/html" svg]]]
             (when (.supports js/ClipboardItem data-type)
               (aset blob-array
                     data-type
                     (js/Blob. (array data) #js {:type data-type}))))
           blob-array)
         (js/ClipboardItem.)
         (array)
         (js/navigator.clipboard.write)
         (.catch #(some-> on-error (conj %) rf/dispatch))))))

(defn text->data
  [text]
  (when (not (string/blank? text))
    {:elements [{:tag :text
                 :content text}]}))

(defn item-type-fn
  [item mime parser]
  [(fn []
     (-> item
         (.getType mime)
         (.then #(.text %))
         (.then parser)))])

(defn item-data-fns
  [item]
  (let [types (set (.-types item))
        file-mime (some (fn [mime] (when (contains? types mime) mime))
                        (keys config/supported-mime-types))]
    (cond
      (contains? types custom-mime-type)
      (item-type-fn item custom-mime-type payload->data)

      (some? file-mime)
      [(file-data-fn item file-mime)]

      (contains? types "text/html")
      (item-type-fn item "text/html" element.handlers/svg->data)

      (contains? types "text/plain")
      (item-type-fn item "text/plain" text->data))))

(defn normalize-data
  [data]
  (let [files (vec (filter :file data))]
    (if (seq files)
      files
      (first (filter (complement :file) data)))))

(rf/reg-fx
 ::clipboard-read
 (fn [{:keys [on-success on-error]}]
   (-> (js/navigator.clipboard.read)
       (.then (fn [items]
                (let [items (->> (vec items)
                                 (mapcat item-data-fns)
                                 (mapv (fn [f] (f))))]
                  (-> (into-array items)
                      (js/Promise.all)
                      (.then (partial filterv some?))))))
       (.then (fn [data]
                (when-let [payload (normalize-data data)]
                  (rf/dispatch (conj on-success payload)))))
       (.catch #(some-> on-error (conj %) rf/dispatch)))))

(rf/reg-fx
 ::focus-canvas
 (fn []
   (some-> (utils.dom/get-canvas-element)
           (.focus))))

(rf/reg-fx
 ::set-document-attr
 (fn [[k v]]
   (.setAttribute js/window.document.documentElement k v)))

(rf/reg-fx
 ::set-meta
 (fn [[k v]]
   (some-> js/document
           (.querySelector (str "meta[name='" k "']"))
           (.setAttribute "content" v))))

(defn show-legacy-open-file-dialog
  [cb]
  (let [el (js/document.createElement "input")]
    (set! (.-type el) "file")
    (.addEventListener el "change"
                       (fn [e]
                         (.remove el)
                         (cb (first (.. e -target -files)))))
    (.click el)))

(defn- request-permission-and-run
  [mode f {:keys [file-handle]
           :as args}]
  (-> (.requestPermission file-handle #js {:mode mode})
      (.then (fn [result]
               (if (= result "granted")
                 (f args)
                 (rf/dispatch [::app.events/toast
                               :error
                               (i18n.views/t [::permission-denied
                                              "Permission to access the file was
                                               denied."])]))))))

(defn- query-permission-and-run
  [mode f {:keys [file-handle]
           :as args}]
  (-> (.queryPermission file-handle #js {:mode mode})
      (.then (fn [result]
               (if (= result "granted")
                 (f args)
                 (request-permission-and-run mode f args))))))

(defn- write-file
  [{:keys [data on-success on-error formatter file-handle]}]
  (-> (.createWritable file-handle)
      (.then (fn [^js/FileSystemWritableFileStream writable-stream]
               (-> (.write writable-stream data)
                   (.then (fn []
                            (.close writable-stream)
                            (some-> on-success
                                    (conj (cond-> file-handle
                                            formatter
                                            formatter))
                                    rf/dispatch))))))
      (.catch #(some-> on-error (conj %) rf/dispatch))))

(defn- abort-error?
  [error]
  (string/includes? (.-message error) "The user aborted a request."))

(rf/reg-fx
 ::file-save
 (fn [{:keys [options on-error file-handle]
       :as args}]
   (if file-handle
     (query-permission-and-run "readwrite" write-file args)
     (some-> (.-showSaveFilePicker js/window)
             (.call js/window (clj->js options))
             (.then #(write-file (assoc args :file-handle %)))
             (.catch (fn [^js/Error error]
                       (when (and on-error (not (abort-error? error)))
                         (rf/dispatch (conj on-error error)))))))))

(defn- get-file
  [{:keys [on-success on-error file-handle]}]
  (-> (.getFile file-handle)
      (.then #(some-> on-success (conj file-handle %) rf/dispatch))
      (.catch #(some-> on-error (conj %) rf/dispatch))))

(rf/reg-fx
 ::file-open
 (fn [{:keys [options on-error on-success file-handle]
       :as args}]
   (if file-handle
     (query-permission-and-run "readwrite" get-file args)
     (if (.-showOpenFilePicker js/window)
       (-> (.showOpenFilePicker js/window (clj->js options))
           (.then (fn [file-handles]
                    (when on-success
                      (doseq [^js/FileSystemFileHandle file-handle file-handles]
                        (get-file (assoc args :file-handle file-handle))))))
           (.catch (fn [^js/Error error]
                     (when (and on-error (not (abort-error? error)))
                       (rf/dispatch (conj on-error error))))))
       (show-legacy-open-file-dialog #(rf/dispatch (conj on-success nil %)))))))

(rf/reg-fx
 ::file-read-as
 (fn [[^js/File file read-as events]]
   (let [reader (js/FileReader.)]
     (doseq
      [[event {:keys [formatter on-fire]}] events]
       (.addEventListener reader event
                          #(rf/dispatch (conj on-fire
                                              (cond-> (.-result reader)
                                                formatter
                                                formatter)))))
     (case read-as
       :data-url (.readAsDataURL reader file)
       :text (.readAsText reader file)))))

(rf/reg-fx
 ::download
 (fn [{:keys [data title]}]
   (let [blob (js/Blob. [data])
         url (js/URL.createObjectURL blob)
         a (js/document.createElement "a")]
     (.setAttribute a "href" url)
     (.setAttribute a "download" title)
     (.click a)
     (js/window.URL.revokeObjectURL url))))

(rf/reg-fx
 ::scroll-into-view
 (fn [dom-el]
   (some-> dom-el
           (.scrollIntoView #js {:block "nearest"}))))

(rf/reg-fx
 ::scroll-to-bottom
 (fn [dom-el]
   (some->> dom-el
            (.-scrollHeight)
            (set! (.-scrollTop dom-el)))))

(rf/reg-fx
 ::eye-dropper
 (fn [{:keys [on-success on-error]}]
   (-> (js/EyeDropper.)
       (.open)
       (.then #(some-> on-success (conj %) rf/dispatch))
       (.catch #(some-> on-error (conj %) rf/dispatch)))))

(rf/reg-fx
 ::print
 (fn [content]
   (let [print-window (.open js/window)
         document (.-document print-window)]
     (.write document content)
     (.print print-window)
     (.close print-window))))

(rf/reg-fx
 ::open-remote-url
 (fn [url]
   (.open js/window url)))

(rf/reg-fx
 ::add-event-listener
 (fn [[target channel event formatter]]
   (.addEventListener target channel
                      #(rf/dispatch (conj event (cond-> %
                                                  formatter
                                                  formatter))))))

(rf/reg-fx
 ::ipc-send
 (fn [[channel data]]
   (some-> js/window.api
           (.send channel (clj->js data)))))

(rf/reg-fx
 ::ipc-invoke
 (fn [{:keys [channel data formatter on-success on-error]}]
   (some-> js/window.api
           (.invoke channel (clj->js data))
           (.then #(some-> on-success
                           (conj (cond-> % formatter formatter))
                           (rf/dispatch)))
           (.catch #(some-> on-error (conj %) rf/dispatch)))))

(rf/reg-fx
 ::ipc-on
 (fn [[channel listener]]
   (some-> js/window.api
           (.on channel #(rf/dispatch (conj listener %))))))
