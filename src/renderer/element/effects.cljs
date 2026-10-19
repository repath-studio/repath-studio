(ns renderer.element.effects
  (:require
   [re-frame.core :as rf]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]
   [renderer.worker.events :as-alias worker.events]))

(defn decode-image!
  [data-url on-error f]
  (let [image (js/Image.)]
    (set! (.-src image) data-url)
    (-> (.decode image)
        (.then #(f image))
        (.catch #(some-> on-error (conj %) rf/dispatch)))))

(defn data-url->canvas!
  [data-url size on-error f]
  (decode-image!
   data-url
   on-error
   (fn [^js/Image image]
     (let [[w h] size
           canvas (js/OffscreenCanvas. w h)]
       (.drawImage (.getContext canvas "2d") image 0 0 w h)
       (f canvas)))))

(rf/reg-fx
 ::trace
 (fn [{:keys [data on-success on-error]}]
   (doseq [image data]
     (let [data-url (-> image :attrs :href)
           [x y] (:bbox image)
           label (:label image)
           preserve-aspect-ratio (-> image :attrs :preserveAspectRatio)
           w (utils.length/unit->px (-> image :attrs :width))
           h (utils.length/unit->px (-> image :attrs :height))]
       (decode-image!
        data-url
        on-error
        (fn [^js/Image img]
          (let [iw (or (when (pos? (.-naturalWidth img))
                         (.-naturalWidth img)) w)
                ih (or (when (pos? (.-naturalHeight img))
                         (.-naturalHeight img)) h)
                canvas (js/OffscreenCanvas. iw ih)]
            (.drawImage (.getContext canvas "2d") img 0 0 iw ih)
            (rf/dispatch
             [::worker.events/create
              {:action "trace"
               :data {:label label
                      :position [x y]
                      :size (when (and (pos? w) (pos? h)) [w h])
                      :viewBox (str "0 0 " iw " " ih)
                      :preserveAspectRatio preserve-aspect-ratio
                      :image (-> canvas
                                 (.getContext "2d")
                                 (.getImageData 0 0 iw ih))}
               :on-success on-success}]))))))))

(defn- decode-image
  [data-url {:keys [^js/File file parent position on-success on-error]}]
  (let [[x y] position
        image (js/Image.)]
    (set! (.-src image) data-url)
    (-> (.decode image)
        (.then #(let [w (.-width image)
                      h (.-height image)
                      el {:type :element
                          :tag :image
                          :label (.-name file)
                          :attrs {:x x
                                  :y y
                                  :width w
                                  :height h
                                  :href data-url}}
                      el (cond-> el parent (assoc :parent parent))]
                  (some-> on-success (conj el) rf/dispatch)))
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
