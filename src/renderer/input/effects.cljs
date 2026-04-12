(ns renderer.input.effects
  (:require
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.events]
   [renderer.utils.dom :as utils.dom]))

(rf/reg-fx
 ::set-pointer-capture
 (fn [pointer-id]
   (.setPointerCapture (utils.dom/canvas-element!) pointer-id)))

(rf/reg-fx
 ::release-pointer-capture
 (fn [pointer-id]
   (.releasePointerCapture (utils.dom/canvas-element!) pointer-id)))

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
                                            :content %
                                            :attrs {:x x
                                                    :y y}}])))

       "file"
       (let [file (.getAsFile item)
             file-handle (.getAsFileSystemHandle item)]
         (rf/dispatch [::element.events/import-file
                       file-handle file position]))))))
