(ns electron.file
  "File operations using Electron APIs.
   The end goal is to eventually replace this with the File System Access API."
  (:require
   ["electron" :refer [app dialog BrowserWindow]]
   ["fs" :as fs]
   ["path" :as path]
   [clojure.edn :as edn]
   [config :as config]))

(def dialog-options
  {:defaultPath (.getPath app config/default-path)
   :properties ["multiSelections"]
   :filters [{:name config/ext
              :extensions [config/ext]}]})

(defn serialize-document
  [data file-path]
  (pr-str (assoc data
                 :path file-path
                 :title (.basename path file-path))))

(defn write-file!
  [file-path data]
  (let [document (-> (apply dissoc data config/save-info-keys)
                     (pr-str))]
    (-> (.writeFile fs/promises file-path document "utf-8")
        (.then #(-> (select-keys data [:id])
                    (serialize-document file-path))))))

(defn read!
  [file-path]
  (let [data (.readFileSync fs file-path "utf-8")
        document (edn/read-string data)]
    (serialize-document document file-path)))

(defn save-dialog!
  [options]
  (-> (.showSaveDialog dialog (clj->js options))
      (.then (fn [^js/Object result]
               (if (.-canceled result)
                 (js/Promise.reject)
                 (-> result js->clj (get "filePath")))))))

(defn save-as!
  [data]
  (let [document (edn/read-string data)
        file-path (:path document)
        directory (and file-path (.dirname path file-path))
        options (cond-> dialog-options
                  (and directory (.existsSync fs directory))
                  (assoc :defaultPath directory)

                  :always
                  (update :defaultPath #(.join path % (:title document))))]
    (-> (save-dialog! options)
        (.then (fn [file-path]
                 (some-> file-path
                         (write-file! document)))))))

(defn save!
  [data]
  (let [document (edn/read-string data)
        file-path (:path document)]
    (if (and file-path (.existsSync fs file-path))
      (write-file! file-path document)
      (save-as! data))))

(defn open!
  [file-path]
  (if file-path
    (array (read! file-path))
    (-> (.showOpenDialog dialog (clj->js dialog-options))
        (.then (fn [^js/Object result]
                 (if (.-canceled result)
                   (js/Promise.reject)
                   (->> (get (js->clj result) "filePaths")
                        (mapv read!)
                        (clj->js)
                        (js/Promise.resolve))))))))

(defn print!
  [content]
  (let [window (BrowserWindow. #js {:autoHideMenuBar true
                                    :title "Print Preview"})]
    (js/Promise.
     (fn [res rej]
       (.on (.-webContents window) "did-finish-load"
            #(.print
              (.-webContents window)
              #js {}
              (fn [success error]
                (.close window)
                (if success
                  (res)
                  (rej error)))))
       (.loadURL window (str "data:text/html;charset=utf-8," content))))))
