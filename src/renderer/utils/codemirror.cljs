(ns renderer.utils.codemirror
  (:require
   ["@codemirror/state" :refer [EditorSelection]]))

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
  (= (dec (get-lines inst))
     (.-from (get-line inst))))

(defn in-place?
  [^js inst]
  (let [lines (get-lines inst)
        line (get-line inst)]
    (or (= 1 lines)
        (and (= lines (.-number line))
             (= (get-head inst) (.-to line))))))

(defn current-word
  "Returns the current 'word' range according to CodeMirror's `wordChars' list.
   Symbols are excluded from the default regex, so we return the last character
   if it's not `/`."
  [^js inst]
  (let [head (get-head inst)]
    (or (.wordAt (.-state inst) head)
        (let [char-before (.sliceDoc (.-state inst) (dec head) head)]
          (when-not (= char-before "/")
            (.range EditorSelection (dec head) head))))))
