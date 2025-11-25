(ns renderer.element.effects
  (:require
   [re-frame.core :as rf]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]
   [renderer.worker.events :as-alias worker.events]))

(defn data-url->canvas!
  [data-url size on-error f]
  (let [[w h] size
        canvas (js/OffscreenCanvas. w h)
        context (.getContext canvas "2d")
        image (js/Image.)]
    (set! (.-src image) data-url)
    (-> (.decode image)
        (.then #(do (.drawImage context image 0 0 w h)
                    (f canvas)))
        (.catch #(some-> on-error (conj %) rf/dispatch)))))

(rf/reg-fx
 ::trace
 (fn [{:keys [data on-success on-error]}]
   (doseq [image data]
     (let [data-url (-> image :attrs :href)
           [x y] (:bbox image)
           ;; TODO: Handle preserveAspectRatio.
           w (utils.length/unit->px (-> image :attrs :width))
           h (utils.length/unit->px (-> image :attrs :height))]
       (data-url->canvas!
        data-url
        [w h]
        on-error
        (fn [canvas]
          (rf/dispatch
           [::worker.events/create
            {:action "trace"
             :data {:label (:label image)
                    :position [x y]
                    :image (-> canvas
                               (.getContext "2d")
                               (.getImageData 0 0 w h))}
             :on-success on-success}])))))))

(defn- decode-image
  [data-url {:keys [^js/File file position on-success on-error]}]
  (let [[x y] position
        image (js/Image.)]
    (set! (.-src image) data-url)
    (-> (.decode image)
        (.then #(let [w (.-width image)
                      h (.-height image)]
                  (some-> on-success
                          (conj {:type :element
                                 :tag :image
                                 :label (.-name file)
                                 :attrs {:x x
                                         :y y
                                         :width w
                                         :height h
                                         :href data-url}})
                          rf/dispatch)))
        (.catch #(some-> on-error (conj %) rf/dispatch)))))

(rf/reg-fx
 ::import-image
 (fn [{:keys [^js/File file]
       :as args}]
   (let [reader (js/FileReader.)]
     (.addEventListener reader "load" #(decode-image (.-result reader) args))
     (.readAsDataURL reader file))))

(rf/reg-fx
 ::export-image
 (fn [{:keys [mime-type size quality data on-success on-error]}]
   (let [data-url (str "data:image/svg+xml," (js/encodeURIComponent data))]
     (data-url->canvas!
      data-url
      size
      on-error
      (fn [canvas]
        (-> canvas
            (.convertToBlob #js {:type mime-type
                                 :quality (or quality 1)})
            (.then #(some-> on-success (conj % mime-type) rf/dispatch))
            (.catch #(some-> on-error (conj %) rf/dispatch))))))))

(rf/reg-fx
 ::->path
 (fn [{:keys [data on-success on-error]}]
   (-> (mapv utils.element/->path data)
       (js/Promise.all)
       (.then #(some-> on-success (conj %) rf/dispatch))
       (.catch #(some-> on-error (conj %) rf/dispatch)))))
