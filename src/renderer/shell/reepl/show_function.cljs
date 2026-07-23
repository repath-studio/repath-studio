(ns renderer.shell.reepl.show-function
  (:require
   [clojure.string :as string]))

(def cljs-fn-prefix
  "cljs$core$IFn$_invoke$arity$")

(defn recover-cljs-name
  [parts]
  (-> (string/join \. (butlast parts))
      (str \/ (last parts))
      demunge))

(defn get-cljs-arities
  [f]
  (map
   #(aget f %)
   (filter #(.startsWith % cljs-fn-prefix) (js->clj (js/Object.keys f)))))

(defn get-fn-summary
  [f]
  (let [source (str f)
        args (second (re-find #"\(([^\)]+)\)" source))]
    (map demunge
         (string/split args \,))))

(defn get-function-forms
  [f]
  (let [arities (get-cljs-arities f)
        arities (if (empty? arities)
                  [f]
                  arities)]
    (map get-fn-summary
         arities)))

(defn get-fn-name
  [f]
  (let [parts (.split (.-name f) \$)]
    (cond
      (empty? (.-name f)) "*anonymous*"
      (= 1 (count parts)) (.-name f)
      :else (recover-cljs-name parts))))

(defn str-fn-forms
  [forms]
  (str \[ (string/join "] [" (map (partial string/join " ") forms)) \]))

(defn show-fn-with-docs
  [get-doc f _ _]
  (when (= js/Function (type f))
    (let [docs (get-doc (symbol (get-fn-name f)))
          native-fn? (.match (str f) #"\{ \[native code\] \}$")]
      [:div.inline-block.shrink-0.box-border
       (if docs
         [:span.function-docs docs]
         [:<>
          [:span.function-head "fn " (get-fn-name f)]
          [:span.function-arities (str-fn-forms (get-function-forms f))]
          [:span.function-body (when native-fn? "[native code]")]])])))
