(ns electron.electron-test
  "Integration tests for the Electron main process."
  (:require
   [cljs.test :refer-macros [deftest is testing async]]
   [clojure.edn :as edn]))

(defn- api
  "The IPC bridge exposed by the production preload."
  []
  (or js/window.api
      (throw (js/Error. "window.api missing: is preload.js running?"))))

(deftest open-documents
  (testing "reads a document through the real main process"
    (async done
           (-> (.. (api) (invoke "test-fixture-path"))
               (.then (fn [fixture-path]
                        (is (string? fixture-path))
                        (.. (api) (invoke "open-documents" fixture-path))))
               (.then (fn [documents]
                        (let [doc (edn/read-string (aget documents 0))]
                          (is (= 1 (.-length documents)))
                          (is (= "fixture.rps" (:title doc)))
                          (is (string? (:path doc)))
                          (is (= {:tag :circle
                                  :attrs {:r "100"
                                          :cx "100"
                                          :cy "100"}}
                                 (-> doc :elements vals first))))
                        (done)))
               (.catch (fn [err]
                         (is false (str "open-documents: " (.-message err)))
                         (done)))))))

(deftest open-missing-document
  (testing "returns nil for a path that does not exist or is not a file"
    (async done
           (-> (.. (api) (invoke "open-documents"
                                 "/nonexistent/document-test.rps"))
               (.then (fn [documents]
                        (is (= 1 (.-length documents)))
                        (is (nil? (aget documents 0)))
                        (.. (api) (invoke "test-fixture-dir"))))
               (.then (fn [fixture-dir]
                        (.. (api) (invoke "open-documents" fixture-dir))))
               (.then (fn [documents]
                        (is (= 1 (.-length documents)))
                        (is (nil? (aget documents 0)))
                        (done)))
               (.catch (fn [err]
                         (is false
                             (str "open-documents failed: " (.-message err)))
                         (done)))))))

(deftest save-document
  (testing "writes a document through the real main process"
    (async done
           (let [fixture-path (volatile! nil)
                 save-id (random-uuid)
                 save-data
                 (fn [path]
                   (pr-str {:id save-id
                            :title "fixture.rps"
                            :path path
                            :elements {:e2 {:tag :rect
                                            :attrs {:width "100"
                                                    :height "100"}}}}))]
             (-> (.. (api) (invoke "test-fixture-path"))
                 (.then (fn [path]
                          (vreset! fixture-path path)
                          (.. (api) (invoke "save-document" (save-data path)))))
                 (.then (fn [saved]
                          (let [saved-doc (edn/read-string saved)]
                            (is (= save-id (:id saved-doc)))
                            (is (= "fixture.rps" (:title saved-doc)))
                            (is (= @fixture-path (:path saved-doc))))
                          (.. (api) (invoke "open-documents" @fixture-path))))
                 (.then (fn [documents]
                          (let [doc (edn/read-string (aget documents 0))]
                            ;; Saving strips config/save-info-keys.
                            (is (nil? (:id doc)))
                            (is (= {:tag :rect
                                    :attrs {:width "100"
                                            :height "100"}}
                                   (get-in doc [:elements :e2]))))
                          (done)))
                 (.catch (fn [err]
                           (is false (str "save-document: " (.-message err)))
                           (done))))))))

(deftest fixture-cleanup
  (testing "removes the on-disk fixture, which is recreated lazily"
    (async done
           (-> (.. (api) (invoke "test-fixture-cleanup"))
               (.then (fn [ok]
                        (is (= true ok))
                        (done)))
               (.catch (fn [err]
                         (is false
                             (str "fixture-cleanup failed: " (.-message err)))
                         (done)))))))
