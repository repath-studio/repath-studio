(ns renderer.shell.impl.javascript
  (:require
   ["@codemirror/lang-javascript" :refer [javascript]]
   ["@codemirror/state" :refer [EditorState]]
   [camel-snake-kebab.core :as camel-snake-kebab]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.hierarchy :as hierarchy]
   [renderer.shell.effects :refer [log]]
   [renderer.shell.events :as-alias shell.events]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.reepl.sci :as shell.reepl.sci]
   [renderer.shell.subs :as-alias shell.subs]
   [user]))

(hierarchy/derive! :js ::shell.hierarchy/language)

(defn expose-command-to-global-namespace
  [command]
  (let [fn-val @command
        wrapper (fn [& args]
                  (apply fn-val (map #(js->clj % :keywordize-keys true)
                                     args)))]
    (aset js/window
          (camel-snake-kebab/->camelCaseString (:name (meta command)))
          wrapper)))

(defmethod shell.hierarchy/init :js
  [{:keys [on-success]}]
  ;; Expose all user functions to global namespace.
  (doseq [command (vals (ns-publics 'user))]
    (expose-command-to-global-namespace command))
  (shell.reepl.sci/refresh-global-names!)

  (rf/dispatch on-success))

(defmethod shell.hierarchy/help :js
  [_language command]
  (if-let [f (get (ns-publics 'user) (symbol command))]
    (log [:command (camel-snake-kebab/->camelCaseString (:name (meta f)))]
         " - "
         (:doc (meta f)))
    (log "Command not found:" command)))

(defmethod shell.hierarchy/welcome :js
  [_language]
  (log "Type " [:command "help()"] " to see a list of commands."))

(defmethod shell.hierarchy/evaluate :js
  [_language s]
  (str "(js/eval \""
       (-> s
           (string/replace "\\" "\\\\")
           (string/replace "\"" "\\\""))
       "\")"))

(defmethod shell.hierarchy/codemirror-options :js
  [_language]
  {:extensions [(.of EditorState.languageData
                     (fn [] #js [#js {:wordChars "."}]))
                (javascript)]})

(defmethod shell.hierarchy/parser :js
  [_language]
  (.. (javascript) -language -parser))

(defn- command-sym
  [publics text]
  (when-not (string/includes? text ".")
    (let [sym (symbol (camel-snake-kebab/->kebab-case-string text))]
      (when (contains? publics sym)
        sym))))

(defmethod shell.hierarchy/completions :js
  [_language s]
  (when-let [completions (shell.reepl.sci/js-completion s "")]
    (let [publics (ns-publics 'user)]
      (mapv (fn [word]
              (if-let [sym (command-sym publics (second word))]
                [sym (second word)]
                word))
            completions))))

(defn- js-arg
  [arg]
  (cond (symbol? arg) (name arg)
        (vector? arg) (str "[" (string/join ", " (map js-arg arg)) "]")
        (and (map? arg) (:as arg)) (name (:as arg))
        :else (pr-str arg)))

(defn- js-arglist
  "Converts a Clojure arglist to a JavaScript style signature.
   E.g. `[[cx cy] r & {:as attrs}]` becomes `([[cx, cy]], r, attrs)`."
  [arglist]
  (let [rest-pos (reduce-kv (fn [i k v] (or i (when (= '& v) k))) nil arglist)
        args (take (or rest-pos (count arglist)) arglist)
        rest-arg (when rest-pos (nth arglist (inc rest-pos)))]
    (str "(" (string/join ", "
                          (map js-arg
                               (concat args
                                       (when (some? rest-arg) [rest-arg]))))
         ")")))

(defmethod shell.hierarchy/docs :js
  [_language s]
  (when (symbol? s)
    (when-let [doc (shell.reepl.sci/doc-from-sym
                    (symbol "user" (name s)))]
      (with-out-str
        (shell.reepl.sci/print-language-doc
         (assoc doc :name
                (camel-snake-kebab/->camelCaseString (name s)))
         js-arglist)))))

(defmethod shell.hierarchy/show-error :js
  [_language v]
  (str (when-let [error-type (:type (last (:via v)))]
         (str (name (keyword error-type)) ": "))
       (:cause v)))

(rf/dispatch [::action.events/register-action
              {:id :shell-language/javascript
               :icon "javascript"
               :label [::label "JavaScript"]
               :event [::shell.events/activate-language :js]
               :active [::shell.subs/active-language? :js]}])
