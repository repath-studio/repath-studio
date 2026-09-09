(ns renderer.utils.codemirror)

(defn get-head
  [^js inst]
  (.. inst -state -selection -main -head))

(defn get-line
  [^js inst]
  (->> (get-head inst)
       (.lineAt (.. inst -state -doc))))

(defn get-lines
  [^js inst]
  (.. inst -state -doc -lines))

(defn get-length
  [^js inst]
  (.. inst -state -doc -length))

(defn set-value
  [^js inst value]
  (.dispatch inst #js {:changes #js {:from 0
                                     :to (get-length inst)
                                     :insert value}}))

(defn first-line?
  [^js inst]
  (zero? (.-from (get-line inst))))

(defn last-line?
  [^js inst]
  (= (get-lines inst)
     (.-number (get-line inst))))

(defn in-place?
  [^js inst]
  (let [lines (get-lines inst)
        line (get-line inst)]
    (or (= 1 lines)
        (and (= lines (.-number line))
             (= (get-head inst) (.-to line))))))

(defn current-word
  [^js inst]
  (let [head (get-head inst)]
    (.wordAt (.-state inst) head)))
