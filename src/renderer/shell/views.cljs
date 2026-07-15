(ns renderer.shell.views
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.action.views :as action.views]
   [renderer.events :as-alias events]
   [renderer.i18n.views :as i18n.views]
   [renderer.panel.events :as-alias panel.events]
   [renderer.panel.subs :as-alias panel.subs]
   [renderer.panel.views :as panel.views]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.reepl.codemirror :as codemirror]
   [renderer.shell.reepl.handlers :as reepl.handlers]
   [renderer.shell.reepl.replumb :as reepl.replumb]
   [renderer.shell.reepl.show-devtools :as show-devtools]
   [renderer.shell.reepl.show-function :as show-function]
   [renderer.shell.reepl.show-value :refer [show-value]]
   [renderer.shell.subs :as-alias shell.subs]
   [renderer.theme.subs :as-alias theme.subs]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]
   [replumb.core :as replumb])
  (:require-macros
   [reagent.ratom :refer [reaction]]))

(def initial-state
  {:items []
   :hist-pos 0
   :history [""]})

(defn get-items
  [db]
  (reaction (:items @db)))

(defn current-text
  [db]
  (let [idx (reaction (:hist-pos @db))
        history (reaction (:history @db))]
    (reaction (let [history @history
                    pos (- (count history) @idx 1)]
                {:pos pos
                 :count (count history)
                 :text (get history pos)}))))

(defn language-dropdown-button
  []
  (let [active-language @(rf/subscribe [::shell.subs/active-language])
        action-group (action.views/deref-action-group :shell/languages)
        {:keys [actions label]} action-group]
    [:> DropdownMenu/Root
     [:> DropdownMenu/Trigger
      {:as-child true}
      [:button.form-control-button.font-mono.px-2!
       {:title (i18n.views/t label)}
       (string/upper-case (name active-language))]]
     [:> DropdownMenu/Portal
      (->> actions
           (map views/dropdown-menu-item)
           (into [:> DropdownMenu/Content
                  {:side "top"
                   :align "end"
                   :class "menu-content rounded-sm"
                   :on-key-down #(.stopPropagation %)
                   :on-escape-key-down #(.stopPropagation %)}
                  [views/dropdownmenu-arrow]]))]]))

(defn repl-input
  [state submit cm-opts]
  {:pre [(every? (comp not nil?)
                 (map cm-opts
                      [:on-up
                       :on-down
                       :complete-atom
                       :complete-word
                       :on-change]))]}
  (let [{:keys [_pos _count _text]} @state
        repl-history? @(rf/subscribe [::panel.subs/visible? :repl-history])
        loaded? @(rf/subscribe [::shell.subs/language-loaded?])]
    [:div.flex.items-center
     [:div.flex.text-xs.self-start.p-1.5
      {:class "m-0.5"}
      (if loaded?
        (replumb/get-prompt)
        [:span.text-foreground-disabled
         (i18n.views/t "Loading language...")])]
     ^{:key (str (hash (:js-cm-opts cm-opts)))}
     [codemirror/code-mirror
      (reaction (:text @state))
      (merge {:on-eval submit
              :readOnly (not loaded?)} cm-opts)]
     [:div.self-start.h-full.flex.items-center.gap-px
      [language-dropdown-button]
      (when @(rf/subscribe [::window.subs/md?])
        [:div.self-start.flex
         [:button.form-control-button
          {:title (i18n.views/t
                   (if repl-history?
                     [::hide-command-output "Hide command output"]
                     [::show-command-output "Show command output"]))
           :on-click #(rf/dispatch [::panel.events/toggle
                                    :repl-history])}
          [views/icon (if repl-history? "chevron-down" "chevron-up")]]])]]))

(defmulti item (fn [i _opts] (:type i)))

(defmethod item :input
  [{:keys [_num text theme]} opts]
  [:div.text-foreground-disabled.font-bold "=>"]
  [:div.flex-1.cursor-pointer.break-words
   {:on-click #((:set-text opts) text)}
   [codemirror/colored-text text theme]])

(defmethod item :log
  [{:keys [value]} opts]
  [show-value value nil opts])

(defmethod item :error
  [{:keys [value]} _opts]
  (let [message (.-message value)
        underlying (.-cause value)]
    [:span.text-error
     message
     (when underlying
       ;; TODO: also show stack?
       [:span.ml-2 (.-message underlying)])]))

(defmethod item :output
  [{:keys [value]} opts]
  [:div.flex-1.break-words [show-value value nil opts]])

(defn repl-items
  [items opts]
  (let [loaded? @(rf/subscribe [::shell.subs/language-loaded?])]
    [:div.flex-1.border-b.border-border.h-full.overflow-hidden.flex
     (if loaded?
       [views/scroll-area
        {:ref #(rf/dispatch [::events/scroll-to-bottom %])}
        (->> items
             (map (fn [i]
                    [:div.font-mono.p-1.flex.text-xs.min-h-4 [item i opts]]))
             (into [:div.p-1 {:dir "ltr"}]))]
       [:div.flex.items-center.justify-center.h-full.w-full
        [views/loading-indicator]])]))

(defn completion-item
  [text selected active set-active]
  [:div.p-1.bg-secondary.text-nowrap
   {:ref #(when selected (rf/dispatch [::events/scroll-into-view %]))
    :on-pointer-enter set-active
    :class (when selected (if active
                            "bg-accent! text-accent-foreground!"
                            "bg-primary!"))}
   text])

(defn function-docs
  [s]
  (let [codemirror-theme @(rf/subscribe [::theme.subs/codemirror])
        [fn-name signature doc] (filter seq (string/split-lines s))]
    [:div.bg-primary.drop-shadow.p-4.absolute.bottom-full.flex.flex-col.gap-4
     [:div.font-semibold fn-name]
     (when signature [codemirror/colored-text signature codemirror-theme])
     (when doc [:div doc])]))

(defn completion-list
  [docs {:keys [pos words active show-all]} set-active]
  (let [items (map-indexed #(vector completion-item
                                    (get %2 2)
                                    (= %1 pos)
                                    active
                                    (partial set-active %1)) words)]
    [:div#completion-list.absolute.bottom-full.left-0.w-full.text-xs.mb-px
     (when docs
       [function-docs docs])
     (into
      [:div.overflow-hidden.flex
       {:class (when show-all "flex-wrap")}]
      items)]))

(defn maybe-fn-docs
  [f]
  (let [doc (reepl.replumb/doc-from-sym f)]
    (when (:forms doc)
      (with-out-str
        (reepl.replumb/print-doc doc)))))

(defn set-print!
  [log]
  (set! cljs.core/*print-newline* false)
  (set! cljs.core/*print-err-fn*
        (fn [& args]
          (if (= 1 (count args))
            (log (first args))
            (log args))))
  (set! cljs.core/*print-fn*
        (fn [& args]
          (if (= 1 (count args))
            (log (first args))
            (log args)))))

(defn repl
  [& {:keys [execute
             complete-word
             get-docs
             state
             show-value-opts
             js-cm-opts
             on-cm-init]}]
  (reagent/with-let [state (or state
                               (reagent/atom initial-state))
                     {:keys [add-input
                             add-result
                             go-up
                             go-down
                             clear-items
                             set-text
                             add-log]} (reepl.handlers/make-handlers state)
                     items (get-items state)
                     complete-atom (reagent/atom nil)
                     docs (reaction
                           (when-let [state @complete-atom]
                             (let [{:keys [pos words]} state
                                   sym (first (get words pos))]
                               (when (symbol? sym)
                                 (get-docs sym)))))
                     submit (fn [text]
                              (if (= "clear" (.trim text))
                                (do
                                  (clear-items)
                                  (set-text ""))
                                (when (pos? (count (.trim text)))
                                  (set-text text)
                                  (add-input text)
                                  (execute text #(add-result (not %1) %2)))))]

    (set-print! add-log)
    [:<>
     (when (and @(rf/subscribe [::panel.subs/visible? :repl-history])
                @(rf/subscribe [::window.subs/md?]))
       [:<>
        [panel.views/separator]
        [panel.views/panel
         {:id :repl-history
          :class "relative"
          :minSize 100
          :defaultSize 300}
         [repl-items @items (assoc show-value-opts
                                   :set-text set-text
                                   :theme (:theme js-cm-opts))]
         [panel.views/close-button :repl-history]]])

     (when-not @(rf/subscribe [::window.subs/md?])
       [repl-items @items (assoc show-value-opts
                                 :set-text set-text
                                 :theme (:theme js-cm-opts))])

     [:div.relative.whitespace-pre-wrap.font-mono.w-full
      {:dir "ltr"}
      [completion-list
       @docs
       @complete-atom
       #(swap! complete-atom assoc :pos % :active true)]
      (let [_items @items] ; TODO: This needs to be removed
        [repl-input
         (current-text state)
         submit
         {:complete-word complete-word
          :on-up go-up
          :on-down go-down
          :complete-atom complete-atom
          :on-change set-text
          :js-cm-opts js-cm-opts
          :on-cm-init on-cm-init}])]]))

(defonce state (reagent/atom initial-state))

(defn root
  []
  (let [language (rf/subscribe [::shell.subs/active-language])
        verbose? (rf/subscribe [::shell.subs/verbose?])
        codemirror-theme @(rf/subscribe [::theme.subs/codemirror])]
    [repl
     :execute #(reepl.replumb/run-repl (shell.hierarchy/evaluate @language %)
                                       {:verbose @verbose?}
                                       %2)
     :complete-word #(reepl.replumb/process-apropos @language %)
     :get-docs reepl.replumb/process-doc
     :state state
     :show-value-opts
     {:showers [show-devtools/show-devtools
                (partial show-function/show-fn-with-docs maybe-fn-docs)]}
     :js-cm-opts {:mode (shell.hierarchy/codemirror-mode @language)
                  :keyMap "default"
                  :showCursorWhenSelecting true
                  :theme codemirror-theme}]))
