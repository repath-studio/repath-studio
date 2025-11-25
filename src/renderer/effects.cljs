(ns renderer.effects
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.i18n.views :as i18n.views]
   [renderer.utils.dom :as utils.dom]))

(rf/reg-cofx
 ::guid
 (fn [coeffects _]
   (assoc coeffects :guid (random-uuid))))

(rf/reg-fx
 ::clipboard-write
 (fn [{:keys [data on-success on-error]}]
   (-> (let [blob-array (js-obj)]
         (doseq
          [[data-type data] [["image/svg+xml" data]
                             ["text/html" data]]]
           (when (.supports js/ClipboardItem data-type)
             (aset blob-array
                   data-type
                   (js/Blob. (array data) #js {:type data-type}))))
         blob-array)
       (js/ClipboardItem.)
       (array)
       (js/navigator.clipboard.write)
       (.then #(some-> on-success rf/dispatch))
       (.catch #(some-> on-error (conj %) rf/dispatch)))))

(rf/reg-fx
 ::focus-canvas
 (fn []
   (some-> (utils.dom/canvas-element!)
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

(defn legacy-file-open!
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
                 (rf/dispatch [::app.events/toast :error
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

(defn- write-file!
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
     (query-permission-and-run "readwrite" write-file! args)
     (some-> (.-showSaveFilePicker js/window)
             (.call js/window (clj->js options))
             (.then #(write-file! (assoc args :file-handle %)))
             (.catch (fn [^js/Error error]
                       (when (and on-error (not (abort-error? error)))
                         (rf/dispatch (conj on-error error)))))))))

(defn- get-file!
  [{:keys [on-success on-error file-handle]}]
  (-> (.getFile file-handle)
      (.then #(some-> on-success (conj file-handle %) rf/dispatch))
      (.catch #(some-> on-error (conj %) rf/dispatch))))

(rf/reg-fx
 ::file-open
 (fn [{:keys [options on-error on-success file-handle]
       :as args}]
   (if file-handle
     (query-permission-and-run "readwrite" get-file! args)
     (if (.-showOpenFilePicker js/window)
       (-> (.showOpenFilePicker js/window (clj->js options))
           (.then (fn [file-handles]
                    (when on-success
                      (doseq [^js/FileSystemFileHandle file-handle file-handles]
                        (get-file! (assoc args :file-handle file-handle))))))
           (.catch (fn [^js/Error error]
                     (when (and on-error (not (abort-error? error)))
                       (rf/dispatch (conj on-error error))))))
       (legacy-file-open! #(rf/dispatch (conj on-success nil %)))))))

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
   (.scrollIntoView dom-el #js {:block "nearest"})))

(rf/reg-fx
 ::scroll-to-bottom
 (fn [dom-el]
   (set! (.-scrollTop dom-el) (.-scrollHeight dom-el))))

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
           (.on channel #(rf/dispatch listener)))))
