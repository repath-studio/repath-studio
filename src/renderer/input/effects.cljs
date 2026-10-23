(ns renderer.input.effects
  (:require
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.events]
   [renderer.utils.dom :as utils.dom]))

(rf/reg-fx
 ::set-pointer-capture
 (fn [pointer-id]
   (some-> (utils.dom/get-canvas-element)
           (.setPointerCapture pointer-id))))

(rf/reg-fx
 ::release-pointer-capture
 (fn [pointer-id]
   (some-> (utils.dom/get-canvas-element)
           (.releasePointerCapture pointer-id))))

(rf/reg-fx
 ::drop
 (fn [[position data-transfer]]
   (doseq [^js/DataTransferItem item (.-items data-transfer)]
     (case (.-kind item)
       "string"
       (let [[x y] position]
         (.getAsString item #(rf/dispatch [::element.events/add
                                           {:type :element
                                            :tag :text
                                            :content [{:tag :text-node
                                                       :attrs {:content %}}]
                                            :attrs {:x x
                                                    :y y}}])))

       "file"
       (let [file (.getAsFile item)]
         (some-> (.getAsFileSystemHandle item)
                 (.then (fn [handle]
                          (rf/dispatch [::element.events/import-file
                                        handle file position])))))))))
